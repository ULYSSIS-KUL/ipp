package org.ulyssis.ipp.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.utils.JedisHelper;
import org.ulyssis.ipp.utils.Serialization;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.net.URI;

/**
 * Responsible for sending status messages to the status channel.
 *
 * The status reporter is threadsafe
 */
public final class StatusReporter {
    private static final Logger LOG = LogManager.getLogger(StatusReporter.class);
    
    private final Jedis jedis;
    private final String statusChannel;

    /**
     * Create a new status reporter for the given Redis URI and status channel.
     *
     * @param redisURI
     *        The URI for the Redis instance to report the status on. This should
     *        be the "own" Redis instance.
     * @param statusChannel
     *        The channel to broadcast the status messages to. This channel is
     *        made specific to the database, so if the `redisUri`
     *        contains database `N`, and the channel is `status`,
     *        then messages will be broadcast on channel `status:N`.
     * @see org.ulyssis.ipp.utils.JedisHelper#dbLocalChannel(String, java.net.URI)
     */
    public StatusReporter(URI redisURI, String statusChannel) {
        this.jedis = JedisHelper.get(redisURI);
        this.statusChannel = JedisHelper.dbLocalChannel(statusChannel, redisURI);
    }

    /**
     * Broadcast a status message.
     *
     * @param message
     *        The message to broadcast
     * @return True if the broadcast was successful, false if the broadcast was unsuccessful.
     *         Failure to broadcast will be logged.
     */
    public synchronized boolean broadcast(StatusMessage message) {
        if (jedis == null)
            return false;
        try {
            jedis.publish(statusChannel.getBytes(), Serialization.getJsonMapper().writeValueAsBytes(message));
            return true;
        } catch (JsonProcessingException e) {
            LOG.error("Error processing message {}", message.getDetails(), e);
            return false;
        } catch (JedisConnectionException e) {
            LOG.error("Couldn't connect to Redis when sending: {}", message.getDetails(), e);
            return false;
        }
    }
}
