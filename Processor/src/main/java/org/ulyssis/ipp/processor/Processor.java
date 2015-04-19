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
package org.ulyssis.ipp.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.config.ReaderConfig;
import org.ulyssis.ipp.config.Team;
import org.ulyssis.ipp.control.CommandProcessor;
import org.ulyssis.ipp.control.commands.AddTagCommand;
import org.ulyssis.ipp.control.commands.CorrectionCommand;
import org.ulyssis.ipp.control.commands.RemoveTagCommand;
import org.ulyssis.ipp.control.commands.SetEndTimeCommand;
import org.ulyssis.ipp.control.commands.SetStartTimeCommand;
import org.ulyssis.ipp.control.commands.SetStatusCommand;
import org.ulyssis.ipp.control.commands.SetStatusMessageCommand;
import org.ulyssis.ipp.control.commands.SetUpdateFrequencyCommand;
import org.ulyssis.ipp.control.handlers.EventCommandHandler;
import org.ulyssis.ipp.control.handlers.PingHandler;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.snapshot.events.AddTagEvent;
import org.ulyssis.ipp.snapshot.events.CorrectionEvent;
import org.ulyssis.ipp.snapshot.events.EndEvent;
import org.ulyssis.ipp.snapshot.events.Event;
import org.ulyssis.ipp.snapshot.events.IdentityEvent;
import org.ulyssis.ipp.snapshot.events.MessageEvent;
import org.ulyssis.ipp.snapshot.events.RemoveTagEvent;
import org.ulyssis.ipp.snapshot.events.StartEvent;
import org.ulyssis.ipp.snapshot.events.StatusChangeEvent;
import org.ulyssis.ipp.snapshot.events.UpdateFrequencyChangeEvent;
import org.ulyssis.ipp.status.StatusMessage;
import org.ulyssis.ipp.status.StatusReporter;
import org.ulyssis.ipp.utils.JedisHelper;
import org.ulyssis.ipp.utils.Serialization;
import org.ulyssis.ipp.TagId;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The processor, well, pulls and processes the updates that come in from the readers.
 *
 * It maintains the state of the system: the number of laps run for each team, their
 * forecasts, etc.
 */
public final class Processor implements Runnable {
    private static final Logger LOG = LogManager.getLogger(Processor.class);

    private final URI jedisUri;
    private final ProcessorOptions options;
    private final Jedis jedis;
    private final BlockingQueue<Event> eventQueue;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentMap<Event, Consumer<Boolean> > eventCallbacks;
    private final StatusReporter statusReporter;
    private final CommandProcessor commandProcessor;
    private final List<Consumer<Processor>> onStartedCallbacks;

    private final List<ReaderListener> listeners;
    private final List<Thread> threads;

    private final List<Event> events;
    private final ArrayList<Snapshot> snapshots;

    public Processor(final ProcessorOptions options) {
        URI uri = options.getRedisUri();
        this.options = options;
        this.jedisUri = uri;
        this.jedis = JedisHelper.get(uri);
        this.eventQueue = new LinkedBlockingQueue<>();
        this.eventCallbacks  = new ConcurrentHashMap<>();
        this.events = new ArrayList<>();
        this.snapshots = new ArrayList<>();
        this.onStartedCallbacks = new CopyOnWriteArrayList<>();
        this.listeners = new ArrayList<>();
        this.threads = new ArrayList<>();
        this.statusReporter = new StatusReporter(uri, Config.getCurrentConfig().getStatusChannel());
        this.commandProcessor = new CommandProcessor(uri, Config.getCurrentConfig().getControlChannel(), statusReporter);
        initCommandProcessor();
        Snapshot snapshot = Snapshot.builder(Instant.MIN).build();
        this.snapshots.add(snapshot);
        if (!restoreFromRedis()) {
            registerInitialTags();
        }
    }

    /**
     * Restore the state from Redis
     *
     * @return Whether the state has been restored from Redis.
     */
    private boolean restoreFromRedis() {
        try {
            Instant now = Instant.now();
            Jedis remoteJedis = jedis;
            if (options.getClone() != null) {
                remoteJedis = JedisHelper.get(options.getClone());
            }
            Response<Set<byte[]>> redisEvents;
            Response<List<byte[]>> redisSnapshots;
            {
                Transaction t = remoteJedis.multi();
                redisEvents = t.zrange("events".getBytes(StandardCharsets.UTF_8), 0, -1);
                redisSnapshots = t.lrange("snapshots".getBytes(), 0L, -1L);
                t.exec();
            }
            List<Event> eventsToSave = new ArrayList<>();
            for (byte[] eventBytes : redisEvents.get()) {
                Event event = Serialization.getJsonMapper().readValue(eventBytes, Event.class);
                eventsToSave.add(event);
                if (event.getTime().isBefore(now)) {
                    events.add(event);
                } else {
                    executorService.schedule(() -> processEvent(event), Duration.between(now, event.getTime()).toMillis(), TimeUnit.MILLISECONDS);
                }
            }
            for (byte[] snapshotBytes : redisSnapshots.get()) {
                Snapshot snapshot = Serialization.getJsonMapper().readValue(snapshotBytes, Snapshot.class);
                snapshots.add(snapshot);
            }
            if (jedis != remoteJedis) {
                Transaction t = jedis.multi();
                for (Event e : eventsToSave) {
                    saveEvent(e, t);
                }
                saveSnapshotsFrom(1, t);
                saveLatestSnapshot(t);
                t.exec();
            }
        } catch (JedisConnectionException | IOException e) {
            LOG.error("Couldn't restore from Redis", e);
            events.clear();
            snapshots.clear();
            snapshots.add(Snapshot.builder(Instant.MIN).build());
            return false;
        }
        return !events.isEmpty();
    }

    private void initCommandProcessor() {
        commandProcessor.addHandler(new PingHandler());
        commandProcessor.addHandler(
                new EventCommandHandler<>(AddTagCommand.class, AddTagEvent::fromCommand, this::queueEvent));
        commandProcessor.addHandler(
                new EventCommandHandler<>(RemoveTagCommand.class, RemoveTagEvent::fromCommand, this::queueEvent));
        commandProcessor.addHandler(
                new EventCommandHandler<>(CorrectionCommand.class, CorrectionEvent::fromCommand, this::queueEvent));
        commandProcessor.addHandler(
                new EventCommandHandler<>(SetStartTimeCommand.class, StartEvent::fromCommand, this::queueEvent));
        commandProcessor.addHandler(
                new EventCommandHandler<>(SetEndTimeCommand.class, EndEvent::fromCommand, this::queueEvent));
        commandProcessor.addHandler(
                new EventCommandHandler<>(SetStatusCommand.class, StatusChangeEvent::fromCommand, this::queueEvent));
        commandProcessor.addHandler(
                new EventCommandHandler<>(SetStatusMessageCommand.class, MessageEvent::fromCommand, this::queueEvent));
        commandProcessor.addHandler(
                new EventCommandHandler<>(SetUpdateFrequencyCommand.class, UpdateFrequencyChangeEvent::fromCommand, this::queueEvent));
    }

    private void registerInitialTags() {
        for (Team team : Config.getCurrentConfig().getTeams()) {
            for (TagId tag : team.getTags()) {
                eventQueue.add(new AddTagEvent(Instant.MIN, tag, team.getTeamNb()));
            }
        }
    }

    public void addOnStartedCallback(Consumer<Processor> onStarted) {
        onStartedCallbacks.add(onStarted);
    }

    private void notifyStarted() {
        onStartedCallbacks.parallelStream().forEach(f -> f.accept(this));
    }

    /**
     * Run the processor. We implement Runnable so that it can
     * be started in a separate thread.
     */
    @Override
    public void run() {
        LOG.info("Spinning up processor!");
        List<ReaderConfig> readers = Config.getCurrentConfig().getReaders();
        for (int i = 0; i < readers.size(); i++) {
            spawnReaderListener(i);
        }
        Thread commandThread = new Thread(commandProcessor);
        threads.add(commandThread);
        commandThread.start();
        notifyStarted();
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Event event = eventQueue.take();
                Instant now = Instant.now();
                executorService.submit(() -> saveEvent(event));
                if (event.getTime().isBefore(now)) {
                    executorService.submit(() -> processEvent(event));
                } else {
                    executorService.schedule(() -> processEvent(event), Duration.between(now, event.getTime()).toMillis(), TimeUnit.MILLISECONDS);
                }
            }
        } catch (InterruptedException ignored) {
        }
        LOG.info("Stopping processor!");
        executorService.shutdownNow();
        commandProcessor.stop();
        listeners.stream().forEach(listener -> {
            try {
                listener.unsubscribe();
            } catch (JedisConnectionException ignored) {
            }
        });
        threads.stream().forEach(thread -> {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        });
        LOG.info("Bye bye!");
    }

    private void spawnReaderListener(int readerId) {
        URI uri = Config.getCurrentConfig().getReader(readerId).getURI();
        String updateChannel = JedisHelper.dbLocalChannel(Config.getCurrentConfig().getUpdateChannel(), uri);
        Jedis subJedis = JedisHelper.get(uri);
        ReaderListener listener = new ReaderListener(readerId, this::queueEvent);
        listeners.add(listener);
        Thread thread = new Thread(() -> subJedis.subscribe(listener, updateChannel));
        threads.add(thread);
        thread.start();
    }

    private void processEvent(Event event) {
        logProcessEvent(event);
        int oldIndex = Integer.MAX_VALUE;
        if (event.unique()) {
            for (int i = 0; i < events.size(); ++i) {
                if (events.get(i).getClass() == event.getClass()) {
                    Event oldEvent = events.get(i);
                    Event idEvent = new IdentityEvent(events.get(i).getTime());
                    events.set(i, idEvent);
                    oldIndex = Integer.min(i, oldIndex);
                    Transaction t = jedis.multi();
                    try {
                        byte[] oldEventBytes = Serialization.getJsonMapper().writeValueAsBytes(oldEvent);
                        byte[] idEventBytes = Serialization.getJsonMapper().writeValueAsBytes(idEvent);
                        t.zrem("events".getBytes(StandardCharsets.UTF_8), oldEventBytes);
                        t.zadd("events".getBytes(StandardCharsets.UTF_8), idEvent.getTime().toEpochMilli(), idEventBytes);
                        t.exec();
                    } catch (JsonProcessingException e) {
                        LOG.fatal("Couldn't replace event with identity event!", e);
                    }
                }
            }
        }
        int i = events.size() - 1;
        for (; i >= 0; i--) {
            if (events.get(i).getTime().equals(event.getTime()) || events.get(i).getTime().isBefore(event.getTime())) {
                break;
            }
        }
        if (i >= 0 && events.get(i).equals(event)) {
            LOG.error("Something went terribly wrong! Two events are the same?");
        }
        i++;
        events.add(i, event);
        i = Integer.min(i, oldIndex);
        int j = recalculateSnapshotsFrom(i);
        Transaction t = jedis.multi();
        boolean result = saveSnapshotsFrom(j, t);
        if (result)
            result = saveLatestSnapshot(t);
        if (result) {
            t.exec();
        } else {
            LOG.error("Something went wrong when saving events and snapshots to Redis, transaction not executed.");
            t.discard();
        }
        // TODO: Provide a sensible message for NEW_SNAPSHOT?
        statusReporter.broadcast(new StatusMessage(StatusMessage.MessageType.NEW_SNAPSHOT,
                String.valueOf(snapshots.size())));
        if (eventCallbacks.containsKey(event)) {
            eventCallbacks.get(event).accept(true);
            eventCallbacks.remove(event);
        }
    }

    private void logProcessEvent(Event event) {
        if (LOG.isDebugEnabled()) {
            try {
                String eventStr = Serialization.getJsonMapper().writeValueAsString(event);
                LOG.debug("Processing event: {}", eventStr);
            } catch (JsonProcessingException ignored) {
            }
        }
    }

    // NOTE: Returns first snapshot, for use in saveSnapshotsFrom
    private int recalculateSnapshotsFrom(int firstEvent) {
        int firstSnapshot = snapshots.size() + 1 - (events.size() - firstEvent);
        if (firstSnapshot <= 0) {
            firstEvent = firstEvent - firstSnapshot + 1;
            firstSnapshot = 1;
        }
        for (int i = firstEvent, j = firstSnapshot; i < events.size(); i++, j++) {
            Snapshot previousSnapshot = snapshots.get(j - 1);
            Snapshot newSnapshot = events.get(i).apply(previousSnapshot);
            if (j < snapshots.size()) {
                snapshots.set(j, newSnapshot);
            } else {
                snapshots.add(newSnapshot);
            }
        }
        return firstSnapshot;
    }

    private void saveEvent(Event e) {
        Transaction t = jedis.multi();
        if (e.unique()) {
            // TODO: We need to already remove the old event!!!!!!!!!
        }
        if (saveEvent(e, t)) {
            t.exec();
        } else {
            t.discard();
        }
    }

    private boolean saveEvent(Event e, Transaction t) {
        try {
            byte[] eventBytes = Serialization.getJsonMapper().writeValueAsBytes(e);
            t.zadd("events".getBytes(StandardCharsets.UTF_8), e.getTime().toEpochMilli(), eventBytes);
        } catch (JsonProcessingException exc) {
            LOG.error("Error serializing event", exc);
            t.discard();
            return false;
        }
        return true;
    }

    private boolean saveSnapshotsFrom(int j, Transaction t) {
        j = j < 1 ? 1 : j;
        if (j == 1) {
            t.del("snapshots");
        } else {
            t.ltrim("snapshots", 0, j - 2);
        }
        for (; j < snapshots.size(); ++j) {
            try {
                byte[] snapshotBytes = Serialization.getJsonMapper().writeValueAsBytes(snapshots.get(j));
                t.rpush("snapshots".getBytes(), snapshotBytes);
            } catch (JsonProcessingException e) {
                LOG.error("Error serializing snapshot", e);
                t.discard();
                return false;
            }
        }
        return true;
    }

    private boolean saveLatestSnapshot(Transaction t) {
        try {
            byte[] snapshot = Serialization.getJsonMapper().writeValueAsBytes(snapshots.get(snapshots.size() - 1));
            t.set("snapshot".getBytes(StandardCharsets.UTF_8), snapshot);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Saving snapshot: {}", new String(snapshot));
            }
        } catch (JsonProcessingException e) {
            LOG.error("Error serializing latest snapshot", e);
            return false;
        }
        return true;
    }

    /**
     * Queue a tag update for processing.
     */
    private void queueEvent(Event event) {
        try {
            eventQueue.put(event);
        } catch (InterruptedException ignored) {
        }
    }

    private void queueEvent(Event event, Consumer<Boolean> callback) {
        try {
            eventCallbacks.put(event, callback);
            eventQueue.put(event);
        } catch (InterruptedException ignored) {
        }
    }
}
