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
package org.ulyssis.ipp.publisher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.processor.Database;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.status.StatusMessage;
import org.ulyssis.ipp.utils.JedisHelper;
import org.ulyssis.ipp.utils.Serialization;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A publisher that uses a database as a source, and
 * listens on Redis for snapshot updates.
 */
public final class DatabasePublisher extends Publisher implements Runnable {
    private static final Logger LOG = LogManager.getLogger(DatabasePublisher.class);

    private static final long MS_BETWEEN_REPEATS = 1000L;

    private final PublisherOptions options;
    private final JedisHelper.BinaryCallBackPubSub pubSub;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private Instant lastUpdateTime = Instant.EPOCH;

    private void updateCallback(byte[] channel, byte[] message) {
        try {
            final StatusMessage statusMessage = Serialization.getJsonMapper().readValue(message, StatusMessage.class);
            if (statusMessage.getType() == StatusMessage.MessageType.NEW_SNAPSHOT) {
                executorService.execute(this::publishLatestScore);
            }
        } catch (IOException e) {
            LOG.error("Couldn't process status message", e);
        }
    }

    private void publishLatestScoreAgain() {
        publishLatestScore(true);
    }

    private void publishLatestScore() {
        publishLatestScore(false);
    }

    private void publishLatestScore(boolean repeat) {
        try {
            Instant updateTime = Instant.now();
            if (repeat && Duration.between(lastUpdateTime, updateTime).toMillis() < MS_BETWEEN_REPEATS / 2) return;
            lastUpdateTime = updateTime;
            try (Connection connection = Database.createConnection(EnumSet.of(Database.ConnectionFlags.READ_ONLY))) {
                Snapshot.loadLatest(connection).ifPresent(snapshot -> outputScore(new Score(snapshot, true)));
            }
        } catch (SQLException e) {
            LOG.error("Failed to fetch snapshot", e);
        } catch (IOException e) {
            LOG.error("Failed to process snapshot", e);
        }
    }

    public DatabasePublisher(PublisherOptions options) {
        super(options);
        this.options = options;
        Database.setDatabaseURI(options.getDatabaseUri());
        pubSub = new JedisHelper.BinaryCallBackPubSub();
        pubSub.addOnMessageListener(this::updateCallback);
    }

    @Override
    public void run() {
        executorService.scheduleAtFixedRate((Runnable) this::publishLatestScoreAgain, 0L, MS_BETWEEN_REPEATS, TimeUnit.MILLISECONDS);
        try {
            Thread thread = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Jedis jedis = JedisHelper.get(options.getRedisUri());
                            jedis.subscribe(pubSub,
                                    JedisHelper.dbLocalChannel(
                                            Config.getCurrentConfig().getStatusChannel(),
                                            options.getRedisUri()
                                    ).getBytes(StandardCharsets.UTF_8));
                        } catch (JedisConnectionException e) {
                            LOG.error("Error connecting to Redis, retrying...", e);
                            Thread.sleep(3L);
                        }
                    }
                } catch (InterruptedException ignored) {}
            });
            thread.start();
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(10L);
                }
            } catch (InterruptedException ignored) {}
            thread.interrupt();
            pubSub.unsubscribe();
            thread.join();
        } catch (InterruptedException ignored) {}
        executorService.shutdownNow();
        cleanup();
    }
}
