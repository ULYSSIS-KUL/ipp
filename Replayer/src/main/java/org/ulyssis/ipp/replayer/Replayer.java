package org.ulyssis.ipp.replayer;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.updates.TagUpdate;
import org.ulyssis.ipp.utils.JedisHelper;
import org.ulyssis.ipp.utils.Serialization;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public final class Replayer implements Runnable {
    private static final Logger LOG = LogManager.getLogger(Replayer.class);
    private final ReplayerOptions options;
    private final Jedis jedis; // TODO: Individual Jedis for each reader? Or not?
    private final String updateChannel;
    private long updateCount = 0;

    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    private final Semaphore sem = new Semaphore(0);

    public Replayer(ReplayerOptions options) {
        this.options = options;
        this.jedis = JedisHelper.get(options.getRedisURI());
        this.updateChannel =
                JedisHelper.dbLocalChannel(Config.getCurrentConfig().getUpdateChannel(), options.getRedisURI());
    }

    @Override
    public void run() {
        Duration offset = null;
        System.out.println("Will run the replayer with the following replay files:");
        Config config = Config.getCurrentConfig();
        int nbReaders = config.getNbReaders();
        SortedMap<Integer,Path> replayMap = options.getReplayMap();
        Collection<SingleReaderReplay> replays = new ArrayList<>();
        for (int i = 0; i < nbReaders; i++) {
            if (replayMap.containsKey(i)) {
                System.out.println(String.format("Reader %d: %s", i, replayMap.get(i).toAbsolutePath()));
            }
        }
        System.out.println("Press any key to continue (or ctrl-C to cancel)");
        try {
            System.in.read();
            for (Map.Entry<Integer, Path> entry : replayMap.entrySet()) {
                replays.add(new SingleReaderReplay(entry.getKey(), entry.getValue()));
            }
            SingleReaderReplay first = null;
            do {
                first = null;
                Optional<Instant> firstInstant = Optional.empty();
                for (SingleReaderReplay replay : replays) {
                    Optional<Instant> instant = replay.nextTime();
                    if (instant.isPresent() && (!firstInstant.isPresent() || instant.get().isBefore(firstInstant.get()))) {
                        firstInstant = instant;
                        first = replay;
                    }
                }
                if (first != null) {
                    if (offset == null) {
                        offset = Duration.between(firstInstant.get(), Instant.now());
                    }
                    TagUpdate update = first.next().get();
                    Instant newUpdateTime = update.getUpdateTime().plus(offset);
                    TagUpdate updateToPush = new TagUpdate(update.getReaderId(),
                            update.getUpdateCount(),
                            newUpdateTime,
                            update.getTag());
                    // TODO: Delay update pushing a bit! Don't push it all at once!
                    service.schedule(() -> pushUpdate(updateToPush),
                            Duration.between(Instant.now(), newUpdateTime).toMillis(), TimeUnit.MILLISECONDS);
                    sem.acquire();
                } else {
                    LOG.info("No more updates! Stopping...");
                }
            } while (first != null && !Thread.currentThread().isInterrupted());
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            LOG.fatal("An unexpected exception occurred!", e);
        }
    }

    /**
     * Push an update to Redis at the given instant with the given tag.
     *
     * TODO: Refactor: split off a "pusher", so we can use it in the integration tests.
     */
    private void pushUpdate(TagUpdate update) {
        try {
            byte[] updateBytes = Serialization.getJsonMapper().writeValueAsBytes(update);
            LOG.debug("Pushing update {}:{} to Redis",
                    update.getReaderId(), update.getUpdateCount());
            LOG.debug("JSON: {}", Serialization.getJsonMapper().writeValueAsString(update));
            try {
                Transaction t = jedis.multi();
                Response<Long> nextUpdateCount = t.rpush("updates".getBytes(), updateBytes);
                t.publish(updateChannel, String.valueOf(updateCount));
                t.exec();
                updateCount = nextUpdateCount.get();
            } catch (JedisConnectionException e) {
                LOG.error("Error pushing update {} to Redis.", update.getUpdateCount(), e);
            }
        } catch (JsonProcessingException e) {
            LOG.error("Error formatting update as JSON", e);
        }
        sem.release();
    }
}
