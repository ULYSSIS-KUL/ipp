package org.ulyssis.ipp.processor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.updates.TagUpdate;
import org.ulyssis.ipp.snapshot.events.Event;
import org.ulyssis.ipp.snapshot.events.TagSeenEvent;
import org.ulyssis.ipp.utils.JedisHelper;
import org.ulyssis.ipp.utils.Serialization;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * A subscriber to a reader's update announcement channel. Responsible
 * for fetching and queueing the updates for a single reader. The Processor
 * will spawn multiple of these: one for each reader.
 */
public final class ReaderListener extends JedisHelper.CallBackPubSub {
    private static final Logger LOG = LogManager.getLogger(ReaderListener.class);

    /** The id of the last update */
    private long lastUpdate;
    /** The Redis connection to the reader */
    private final Jedis remoteJedis;
    /** The processor that this ReaderListener belongs to (and will push updates to) */
    private final Consumer<Event> updateConsumer;

    public ReaderListener(int id, final Consumer<Event> updateConsumer) {
        this.updateConsumer = updateConsumer;
        this.remoteJedis = JedisHelper.get(Config.getCurrentConfig().getReader(id).getURI());
        this.lastUpdate = -1L;
        syncUpdates();

        this.addOnMessageListener(this::onMessageListener);
    }

    /**
     * Fired when an update comes in, will fetch the updates in between, and process them.
     *
     * NOTE: This will usually only process one update when running, but between construction
     *       and starting to listen, updates may have also have arrived.
     */
    private void onMessageListener(String channel, String message) {
        long updateId = Long.parseLong(message, 10);
        List<byte[]> updates = remoteJedis.lrange(Config.getCurrentConfig().getUpdatesList().getBytes(),
                lastUpdate + 1L, updateId);
        for (byte[] update : updates) {
            processMessage(update);
        }
    }

    /**
     * Batch process all updates on startup
     */
    private void syncUpdates() {
        List<byte[]> updates = remoteJedis.lrange(Config.getCurrentConfig().getUpdatesList().getBytes(), 0L, -1L);
        for (byte[] update : updates) {
            processMessage(update);
        }
    }

    /**
     * Turn a JSON-represented message into a TagUpdate object, and queue it for processing.
     */
    private void processMessage(byte[] message) {
        try {
            TagUpdate update = Serialization.getJsonMapper().readValue(message, TagUpdate.class);
            lastUpdate = update.getUpdateCount();
            updateConsumer.accept(new TagSeenEvent(update));
        } catch (IOException e) {
            LOG.error("Couldn't process update {}!", new String(message), e);
        }
    }
}
