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
package org.ulyssis.ipp.snapshot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.status.StatusMessage;
import org.ulyssis.ipp.utils.JedisHelper;
import org.ulyssis.ipp.utils.Serialization;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SnapshotListener implements Runnable {
    private static final Logger LOG = LogManager.getLogger(SnapshotListener.class);

    private final CopyOnWriteArraySet<Consumer<Snapshot>> listeners = new CopyOnWriteArraySet<>();
    private final ConcurrentHashMap<Consumer<Snapshot>, ExecutorService> executors = new ConcurrentHashMap<>();
    private final URI jedisURI;
    private final Jedis jedis;
    private Jedis jedis2;
    private final Jedis jedis3;
    private final JedisHelper.BinaryCallBackPubSub snapshotSubscriber = new JedisHelper.BinaryCallBackPubSub();

    public SnapshotListener(URI uri) {
        jedisURI = uri;
        jedis = JedisHelper.get(uri);
        jedis2 = JedisHelper.get(uri);
        jedis3 = JedisHelper.get(uri);
        snapshotSubscriber.addOnMessageListener(onMessageListener);
    }

    /**
     * Trigger the fetching of a snapshot, even when there is no announcement,
     * listeners will be activated if there is a snapshot.
     */
    public void trigger() {
        synchronized (jedis3) {
            getAndAnnounceSnapshot(jedis3);
        }
    }

    private final BiConsumer<byte[], byte[]> onMessageListener = (channel, message) -> {
        try {
            StatusMessage statusMessage = Serialization.getJsonMapper().readValue(message, StatusMessage.class);
            if (statusMessage.getType() == StatusMessage.MessageType.NEW_SNAPSHOT) {
                getAndAnnounceSnapshot(jedis2);
            }
        } catch (IOException e) {
            LOG.error("Couldn't process status message", e);
        }
    };

    private void getAndAnnounceSnapshot(Jedis jedis) {
        try {
            byte[] snapshotBytes = jedis.get("snapshot".getBytes(StandardCharsets.UTF_8));
            if (snapshotBytes == null) {
            	return;
            }
            Snapshot snapshot = Serialization.getJsonMapper().readValue(snapshotBytes, Snapshot.class);
            listeners.parallelStream().forEach(l -> {
                ExecutorService service = executors.getOrDefault(l, null);
                if (service == null) {
                    l.accept(snapshot);
                } else {
                    service.submit(() -> l.accept(snapshot));
                }
            });
        } catch (JedisConnectionException e) {
            LOG.error("Triggering snapshot retrieve failed due to JedisConnectionException", e);
        } catch (IOException e) {
            LOG.error("Couldn't process snapshot", e);
        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    jedis.subscribe(snapshotSubscriber, JedisHelper.dbLocalChannel(Config.getCurrentConfig().getStatusChannel(), jedisURI).getBytes(StandardCharsets.UTF_8));
                } catch (JedisConnectionException e) {
                    LOG.error("Connection broken, will attempt to reconnect in 3 seconds", e);
                    Thread.sleep(3000L);
                }
            }
        } catch (InterruptedException ignored) {
        }
    }

    public void stop() {
        snapshotSubscriber.unsubscribe();
        Thread.currentThread().interrupt();
    }

    public void addListener(Consumer<Snapshot> listener) {
        listeners.add(listener);
    }

    // TODO: Potential but unlikely concurrency issues
    // Basically, there is no issue as long as you only register a listener once, which is what you would normally do
    public void addListener(Consumer<Snapshot> listener, ExecutorService executorService) {
        executors.put(listener, executorService);
        listeners.add(listener);
    }

    public void removeListener(Consumer<Snapshot> listener) {
        listeners.remove(listener);
        if (executors.containsKey(listener)) {
            executors.remove(listener);
        }
    }
}
