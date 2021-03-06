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
package org.ulyssis.ipp.ui.state;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.control.CommandDispatcher;
import org.ulyssis.ipp.processor.Database;
import org.ulyssis.ipp.publisher.Score;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.status.StatusMessage;
import org.ulyssis.ipp.utils.JedisHelper;
import org.ulyssis.ipp.utils.Serialization;

import redis.clients.jedis.Jedis;
import eu.webtoolkit.jwt.WApplication;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SharedState {
	private static final Logger LOG = LogManager.getLogger(SharedState.class);
	
    private final URI redisUri;
    
    public URI getRedisURI() {
    	return redisUri;
    }
    
    @FunctionalInterface
    public interface SnapshotScoreListener {
    	void newSnapshotAndScore(Snapshot snapshot, Score score, boolean newSnapshot);
    }

    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private final Thread dispatcherThread;
    private final CommandDispatcher commandDispatcher;
    private final JedisHelper.BinaryCallBackPubSub statusSubscriber = new JedisHelper.BinaryCallBackPubSub();
    private final Jedis statusJedis;
    private final Thread statusThread;

    private final ConcurrentMap<WApplication, Set<SnapshotScoreListener>> applicationToScoreListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<WApplication, Set<Consumer<StatusMessage>>> statusMessageListeners = new ConcurrentHashMap<>();
    
    private final BiConsumer<byte[], byte[]> onMessageListener = (channel, message) -> {
        try {
            final StatusMessage statusMessage = Serialization.getJsonMapper().readValue(message, StatusMessage.class);
            service.submit(() -> announceStatus(statusMessage));
        } catch (IOException e) {
            LOG.error("Couldn't process status message", e);
        }
    };
    
    private Snapshot latestSnapshot;
    
    private void resendLatestScore() {
    	if (latestSnapshot == null) return;
    	Score score = new Score(latestSnapshot, false);
    	applicationToScoreListeners.keySet().forEach(app -> {
            Set<SnapshotScoreListener> snapshotListeners = applicationToScoreListeners.get(app);
            WApplication.UpdateLock lock = app.getUpdateLock();
            try {
                for (SnapshotScoreListener l : snapshotListeners) {
                    l.newSnapshotAndScore(latestSnapshot, score, false);
                }
                app.triggerUpdate();
            } catch (Exception e) {
                LOG.error("Exception", e);
            } finally {
                lock.release();
            }
        });
    }
    
    private void announceStatus(StatusMessage message) {
    	Set<WApplication> apps = new HashSet<>();
    	apps.addAll(statusMessageListeners.keySet());
    	apps.addAll(applicationToScoreListeners.keySet());
    	apps.forEach(app -> {
            Set<Consumer<StatusMessage>> listeners = statusMessageListeners.get(app);
            Set<SnapshotScoreListener> snapshotListeners = applicationToScoreListeners.get(app);
            Snapshot snapshot = null;
            Score score = null;
            if (message.getType() == StatusMessage.MessageType.NEW_SNAPSHOT) {
                try (Connection connection = Database.createConnection(EnumSet.of(Database.ConnectionFlags.READ_ONLY))) {
                    Optional<Snapshot> snapshotOptional = Snapshot.loadLatest(connection);
                    if (snapshotOptional.isPresent()) {
                        snapshot = snapshotOptional.get();
                        latestSnapshot = snapshot;
                        score = new Score(snapshot, false);
                    }
                } catch (SQLException e) {
                    LOG.error("Failed to retrieve snapshot due to SQLException", e);
                } catch (IOException e) {
                    LOG.error("Failure processing snapshot", e);
                }
            }
            WApplication.UpdateLock lock = app.getUpdateLock();
            try {
                if (listeners != null) {
                    listeners.forEach(l -> l.accept(message));
                }
                if (snapshotListeners != null && message.getType() == StatusMessage.MessageType.NEW_SNAPSHOT) {
                    for (SnapshotScoreListener l : snapshotListeners) {
                        l.newSnapshotAndScore(snapshot, score, true);
                    }
                }
                app.triggerUpdate();
            } finally {
                lock.release();
            }
        });
    }

    public SharedState(URI redisUri) {
        this.redisUri = redisUri;
        commandDispatcher = new CommandDispatcher(redisUri, Config.getCurrentConfig().getControlChannel(), Config.getCurrentConfig().getStatusChannel());
        dispatcherThread = new Thread(commandDispatcher);
        dispatcherThread.start();
        try (Connection connection = Database.createConnection(EnumSet.of(Database.ConnectionFlags.READ_ONLY))) {
            Optional<Snapshot> snapshot = Snapshot.loadLatest(connection);
            if (snapshot.isPresent()) {
                latestSnapshot = snapshot.get();
            }
        } catch (SQLException e) {
            LOG.error("Couldn't get snapshot due to SQLException", e);
        } catch (IOException e) {
            LOG.error("Couldn't read snapshot", e);
        }
        statusJedis = JedisHelper.get(redisUri);
        statusSubscriber.addOnMessageListener(onMessageListener);
        statusThread = new Thread(() -> {
        	statusJedis.subscribe(statusSubscriber, JedisHelper.dbLocalChannel(Config.getCurrentConfig().getStatusChannel(), redisUri).getBytes(StandardCharsets.UTF_8));
        });
        statusThread.start();
        service.scheduleAtFixedRate(this::resendLatestScore, 0L, 200L, TimeUnit.MILLISECONDS);
    }

    public void addScoreListener(WApplication app, SnapshotScoreListener listener) {
    	Set<SnapshotScoreListener> listeners = new CopyOnWriteArraySet<>();
    	listeners.add(listener);
    	listeners = applicationToScoreListeners.putIfAbsent(app, listeners);
    	if (listeners != null) {
    		// TODO: This is actually still a minor race condition, but I don't think we'll ever get here
    		listeners.add(listener);
    	}
    }

    public void removeScoreListener(WApplication app, SnapshotScoreListener listener) {
    	Set<SnapshotScoreListener> listeners = applicationToScoreListeners.get(app);
    	if (listeners != null) {
    		// TODO: This is actually still a minor race condition, but I don't think we'll ever get here
    		listeners.remove(listener);
    	}
    }
    
    public void addStatusListener(WApplication app, Consumer<StatusMessage> listener) {
    	Set<Consumer<StatusMessage>> listeners = new CopyOnWriteArraySet<>();
    	listeners.add(listener);
    	listeners = statusMessageListeners.putIfAbsent(app, listeners);
    	if (listeners != null) {
    		listeners.add(listener);
    	}
    }
    
    public void removeStatusListener(WApplication app, Consumer<StatusMessage> listener) {
    	Set<Consumer<StatusMessage>> listeners = statusMessageListeners.get(app);
    	if (listeners != null) {
    		listeners.remove(listener);
    	}
    }
    
    public void removeApplication(WApplication app) {
    	applicationToScoreListeners.remove(app);
    	statusMessageListeners.remove(app);
    }
    
    public CommandDispatcher getCommandDispatcher() {
    	return commandDispatcher;
    }

    public void stop() {
        service.shutdownNow();
        dispatcherThread.interrupt();
        try {
        	dispatcherThread.join();
        } catch (InterruptedException ignored) {
        }
        statusSubscriber.unsubscribe();
        statusThread.interrupt();
        try {
        	statusThread.join();
        } catch (InterruptedException ignored) {
        }
    }
}
