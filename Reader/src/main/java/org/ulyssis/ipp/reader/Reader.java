/*
 * Copyright (C) 2014-2015 ULYSSIS VZW
 *
 * This file is part of i++.
 * 
 * i++ is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Affero General Public License
 * as published by the Free Software Foundation. No other versions apply.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.ulyssis.ipp.reader;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.llrp.ltk.generated.messages.RO_ACCESS_REPORT;
import org.llrp.ltk.generated.parameters.TagReportData;
import org.llrp.ltk.types.LLRPMessage;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.config.ReaderConfig;
import org.ulyssis.ipp.control.CommandProcessor;
import org.ulyssis.ipp.control.handlers.PingHandler;
import org.ulyssis.ipp.status.StatusMessage;
import org.ulyssis.ipp.status.StatusReporter;
import org.ulyssis.ipp.updates.TagUpdate;
import org.ulyssis.ipp.utils.JedisHelper;
import org.ulyssis.ipp.utils.Serialization;
import org.ulyssis.ipp.TagId;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

// TODO: Go over all of the error handling, and evaluate if it is appropriate
// TODO: Set some sort of scheduled task for timeouts, try to reinitialize?

public final class Reader implements Runnable {
    private static final Logger LOG = LogManager.getLogger(Reader.class);

    private final CommandProcessor commandProcessor;
    private final ReaderOptions options;
    private final ReaderConfig readerConfig;
    private final StatusReporter statusReporter;
    private final LLRPReader llrpReader;
    private final ScheduledExecutorService executorService;
    private Optional<ByteChannel> replayChannel = Optional.empty();
    private final String updateChannel;

    // NOTE: be careful, Jedis instances are not threadsafe!
    private final Jedis jedis;
    
    private long updateCount = 0L;

    private Instant lastUpdate = Instant.now();
    
    private final Map<TagId, Instant> lastUpdateForTag;
    private boolean speedwayInitialized = false;

    /**
     * Create a new reader and connect to Redis.
     * 
     * options are passed in, rather than
     * accessed through a singleton or such, to improve testability
     * and modularity, and to prevent hidden dependencies and
     * eventual threading issues.
     * 
     * @param options
     *           The command line options to use for this reader.
     */
    public Reader(ReaderOptions options) {
        this.options = options;
        this.readerConfig = Config.getCurrentConfig().getReader(options.getId());
        this.llrpReader = new LLRPReader(this::messageReceived, this::errorOccurred);

        if (readerConfig.getType() == ReaderConfig.Type.SIMULATOR) {
            executorService = Executors.newSingleThreadScheduledExecutor();
        } else {
            executorService = null;
        }

        if (options.getNoRedis()) {
            LOG.info("Not using Redis, setting initial update count to 0.");
            this.updateCount = 0L;
            this.jedis = null;
        } else {
            this.jedis = JedisHelper.get(readerConfig.getURI());
            try {
                this.updateCount = jedis.llen("updates");
            } catch (JedisConnectionException e) {
                LOG.error("Couldn't connect to Jedis when getting update count. Setting 0 instead.", e);
                this.updateCount = 0L; // TODO: Is 0 appropriate?
            }
        }
        String statusChannel = Config.getCurrentConfig().getStatusChannel();
        this.statusReporter = new StatusReporter(readerConfig.getURI(), statusChannel);
        String controlChannel = Config.getCurrentConfig().getControlChannel();
        this.commandProcessor = new CommandProcessor(readerConfig.getURI(), controlChannel, statusReporter);
        commandProcessor.addHandler(new PingHandler());
        this.updateChannel =
                JedisHelper.dbLocalChannel(Config.getCurrentConfig().getUpdateChannel(), readerConfig.getURI());

        options.getReplayFile().ifPresent(replayFile -> {
            try {
                LOG.info("Opening replay file: {}", replayFile);
                ByteChannel channel = Files.newByteChannel(replayFile,
                        StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                this.replayChannel = Optional.of(channel);
            } catch (IOException e) {
                LOG.error("Couldn't open channel for logging to replay file: {}", replayFile, e);
            }
        });

        this.lastUpdateForTag = new HashMap<>();
    }

    /**
     * Run the reader. Reader implements runnable, so that we can
     * do this in its own thread.
     */
    @Override
    public void run() {
        LOG.info("Spinning up reader!");
        ReaderConfig.Type type = Config.getCurrentConfig().getReader(options.getId()).getType();
        if (type == ReaderConfig.Type.LLRP) {
            initSpeedway();
            if (!speedwayInitialized) {
                shutdownHook();
                return;
            }
        } else if (type == ReaderConfig.Type.SIMULATOR) {
            initSimulator();
        }
        Thread commandThread = new Thread(commandProcessor);
        commandThread.start();
        statusReporter.broadcast(new StatusMessage(StatusMessage.MessageType.STARTED_UP,
            String.format("Started up reader %s!", options.getId())));
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Duration maxUpdateInterval = Duration.ofMillis(Config.getCurrentConfig().getMaxUpdateInterval());
                if (maxUpdateInterval.minus(Duration.between(lastUpdate, Instant.now())).isNegative()) {
                    lastUpdate = Instant.now();
                    LOG.warn("No update received in {} seconds!", maxUpdateInterval.getSeconds());
                    statusReporter.broadcast(new StatusMessage(StatusMessage.MessageType.NO_UPDATES,
                            String.format("No update received in %s seconds!", maxUpdateInterval.getSeconds())));
                }
                Thread.sleep(1000L);
            }
        } catch (InterruptedException e) {
            // We don't care about this exception
        }
        commandProcessor.stop();
        commandThread.interrupt();
        try {
            commandThread.join();
        } catch (InterruptedException ignored) {
        }
        shutdownHook();
    }

    private void initSpeedway() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            while (!speedwayInitialized && !Thread.currentThread().isInterrupted()) {
                // We're doing this in another thread because LLRPReader doesn't
                // interrupt properly.
                Callable<Boolean> runCallable =
                        () -> llrpReader.run(Config.getCurrentConfig().getSpeedwayURI(options.getId()));
                Future<Boolean> initFuture = executor.submit(runCallable);
                try {
                    speedwayInitialized = initFuture.get();
                } catch (ExecutionException e) {
                    LOG.error("Starting the Speedway caused an exception.", e);
                    speedwayInitialized = false;
                }
                if (!speedwayInitialized) {
                    LOG.error("Couldn't start the Speedway! Retrying in {} ms.",
                            Config.getCurrentConfig().getRetryInterval());
                    statusReporter.broadcast(new StatusMessage(StatusMessage.MessageType.STARTUP_FAILURE,
                            "Failed to start the Speedway!"));
                    Thread.sleep(Config.getCurrentConfig().getRetryInterval());
                }
            }
        } catch (InterruptedException e) {
            // Shutting down
        }
    }

    private void initSimulator() {
        for (ReaderConfig.SimulatedTeam team : readerConfig.getSimulatedTeams()) {
            Runnable runnable = () -> simulateOneTeam(team);
            double startingPosition = Config.getCurrentConfig().getTrackLength() / 2.0;
            double distanceToGo;
            if (readerConfig.getPosition() > startingPosition) {
                distanceToGo = readerConfig.getPosition() - startingPosition;
            } else {
                distanceToGo =
                        Config.getCurrentConfig().getTrackLength() - startingPosition + readerConfig.getPosition();
            }
            double avgVelocity = Config.getCurrentConfig().getTrackLength() / team.getLapTime();
            double time = distanceToGo / avgVelocity;
            executorService.schedule(runnable, (long) time, TimeUnit.SECONDS);
        }
    }

    private void simulateOneTeam(ReaderConfig.SimulatedTeam team) {
        Instant instant = Instant.now();
        TagId tag = team.getTag();
        if (acceptUpdate(instant, tag)) {
            pushUpdate(instant, tag);
        }
        Runnable runnable = () -> simulateOneTeam(team);
        executorService.schedule(runnable, team.getLapTime(), TimeUnit.SECONDS);
    }

    /**
     * Perform cleanup on shutdown. (When the thread is interrupted.)
     */
    private void shutdownHook() {
        statusReporter.broadcast(new StatusMessage(StatusMessage.MessageType.SHUTDOWN,
            String.format("Shutting down reader %s.", options.getId())));
        if (speedwayInitialized) {
            LOG.info("Shutting down reader!");
            boolean successfulStop = llrpReader.stop();
            if (!successfulStop) {
                LOG.error("Could not stop the Speedway!");
            } else {
                LOG.info("Successfully stopped the reader!");
            }
        }
        replayChannel.ifPresent(channel -> {
            final Path replayFile = options.getReplayFile().get();
            try {
                channel.close();
            } catch (IOException e) {
                LOG.error("Error while closing replay log file: {}.",
                        replayFile, e);
            }
            try {
                if (Files.size(replayFile) == 0L) {
                    LOG.info("Deleting empty replay file: {}", replayFile);
                    Files.delete(replayFile);
                }
            } catch (IOException e) {
               LOG.error("Couldn't check size of replay log {}, or delete it.", replayFile, e);
            }
        });
        LOG.info("Bye bye!");
    }

    // TODO: Recover in case of lost connection with Speedway.
    public void messageReceived(LLRPMessage msg) {
        if (msg.getTypeNum() == RO_ACCESS_REPORT.TYPENUM) {
            RO_ACCESS_REPORT report = (RO_ACCESS_REPORT) msg;
            List<TagReportData> tags = report.getTagReportDataList();
            for (TagReportData tagReportData : tags) {
                // NOTE: We're using Instant.now() instead of relying on the timestamp
                //          of the update. The delay should be small, and we'll keep the
                //         counting systems in sync using NTP.
                Instant now = Instant.now();
                TagId tag = LLRPReader.decodeEPCParameter(tagReportData.getEPCParameter());
                if (acceptUpdate(now, tag)) {
                    pushUpdate(now, tag);
                }
            }
        }
    }

    public void errorOccurred(String s) {
        LOG.error("An error occurred: {}", s);
        statusReporter.broadcast(new StatusMessage(StatusMessage.MessageType.MISC_ERROR, s));
    }

    /**
     * Check whether this tag code has been seen at least config.getMinUpdateInterval()
     * ago. If not, the update should be ignored.
     */
    private boolean acceptUpdate(Instant now, TagId tag) {
        lastUpdate = now;
        boolean result = !lastUpdateForTag.containsKey(tag) ||
                Duration.ofMillis(Config.getCurrentConfig().getMinUpdateInterval()).minus(
                        Duration.between(lastUpdateForTag.get(tag), now)).isNegative();
        if (result) {
            lastUpdateForTag.put(tag, now);
        }
        return result;
    }

    /**
     * Push an update to Redis at the given instant with the given tag.
     *
     * TODO: Refactor: split off a "pusher", so we can use it in the integration tests.
     */
    private void pushUpdate(Instant now, TagId tag) {
        TagUpdate update = new TagUpdate(options.getId(), updateCount, now, tag);
        try {
            byte[] updateBytes = Serialization.getJsonMapper().writeValueAsBytes(update);
            logUpdate(updateBytes);
            if (options.getNoRedis()) {
                updateCount++;
            } else {
                LOG.debug("Pushing update {}:{} to Redis",
                        update.getReaderId(), update.getUpdateCount());
                try {
                    Transaction t = jedis.multi();
                    Response<Long> nextUpdateCount = t.rpush("updates".getBytes(), updateBytes);
                    t.publish(updateChannel, String.valueOf(updateCount));
                    t.exec();
                    updateCount = nextUpdateCount.get();
                } catch (JedisConnectionException e) {
                    LOG.error("Error pushing update {} to Redis.", update.getUpdateCount(), e);
                }
            }
        } catch (JsonProcessingException e) {
            LOG.error("Error formatting update as JSON", e);
        }
    }

    /**
     *  Log an update to the replay log, if enabled.
     */
    private void logUpdate(byte[] updateBytes) {
        LOG.debug("Update: {}", new String(updateBytes));
        replayChannel.ifPresent(channel -> {
            try {
                channel.write(ByteBuffer.wrap(updateBytes));
                byte[] newLine = "\n".getBytes();
                channel.write(ByteBuffer.wrap(newLine));
            } catch (IOException e) {
                LOG.error("Couldn't log to replay file: {}",
                        options.getReplayFile().get(), e);
            }
        });
    }

}
