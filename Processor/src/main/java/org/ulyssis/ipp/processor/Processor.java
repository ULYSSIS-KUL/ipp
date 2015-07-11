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
import org.ulyssis.ipp.snapshot.AddTagEvent;
import org.ulyssis.ipp.snapshot.CorrectionEvent;
import org.ulyssis.ipp.snapshot.EndEvent;
import org.ulyssis.ipp.snapshot.Event;
import org.ulyssis.ipp.snapshot.MessageEvent;
import org.ulyssis.ipp.snapshot.RemoveTagEvent;
import org.ulyssis.ipp.snapshot.StartEvent;
import org.ulyssis.ipp.snapshot.StatusChangeEvent;
import org.ulyssis.ipp.snapshot.UpdateFrequencyChangeEvent;
import org.ulyssis.ipp.status.StatusMessage;
import org.ulyssis.ipp.status.StatusReporter;
import org.ulyssis.ipp.utils.JedisHelper;
import org.ulyssis.ipp.utils.Serialization;
import org.ulyssis.ipp.TagId;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.net.URI;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.ulyssis.ipp.processor.Database.ConnectionFlags.READ_ONLY;
import static org.ulyssis.ipp.processor.Database.ConnectionFlags.READ_WRITE;

/**
 * The processor, well, pulls and processes the updates that come in from the readers.
 *
 * It maintains the state of the system: the number of laps run for each team, their
 * forecasts, etc.
 */
public final class Processor implements Runnable {
    private static final Logger LOG = LogManager.getLogger(Processor.class);

    private final BlockingQueue<Event> eventQueue;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private final ConcurrentMap<Event, Consumer<Boolean> > eventCallbacks;

    private final StatusReporter statusReporter;
    private final CommandProcessor commandProcessor;
    private final List<Consumer<Processor>> onStartedCallbacks;

    /**
     * The listeners responsible for fetching results from all registered readers
     */
    private final List<ReaderListener> readerListeners;

    private final List<Thread> threads;

    private Snapshot snapshot;

    public Processor(final ProcessorOptions options) {
        Database.setDatabaseURI(options.getDatabaseUri());
        if (options.shouldClearDb()) {
            try (Connection connection = Database.createConnection(EnumSet.of(READ_WRITE))) {
                Database.initDb(connection);
                connection.commit();
            } catch (SQLException e) {
                LOG.fatal("Error initializing database!", e);
            }
        }
        URI uri = options.getRedisUri();
        this.eventQueue = new LinkedBlockingQueue<>();
        this.eventCallbacks  = new ConcurrentHashMap<>();
        this.onStartedCallbacks = new CopyOnWriteArrayList<>();
        this.readerListeners = new ArrayList<>();
        this.threads = new ArrayList<>();
        // TODO: Move status reporting and processing of commands to ZeroMQ?
        // Also: post some stuff to a log in the db?
        this.statusReporter = new StatusReporter(uri, Config.getCurrentConfig().getStatusChannel());
        this.commandProcessor = new CommandProcessor(uri, Config.getCurrentConfig().getControlChannel(), statusReporter);
        initCommandProcessor();
        snapshot = new Snapshot(Instant.EPOCH);
        if (!restoreFromDb()) {
            registerInitialTags();
        }
    }

    /**
     * Restore the state from the database
     *
     * @return Whether we could restore from db, if false, we're starting from a clean slate
     */
    private boolean restoreFromDb() {
        Connection connection = null;
        Snapshot oldSnapshot = this.snapshot;
        try {
            connection = Database.createConnection(EnumSet.of(READ_WRITE));
            Optional<Snapshot> snapshot = Snapshot.loadLatest(connection);
            if (snapshot.isPresent()) {
                this.snapshot = snapshot.get();
                connection.commit();
                return true;
            } else {
                List<Event> events = Event.loadAll(connection);
                Snapshot snapshotBefore = this.snapshot;
                // Instant now = Instant.now(); // TODO: Handle future events later!
                for (Event event : events) {
                    if (!event.isRemoved()/* && event.getTime().isBefore(now)*/) { // TODO: Future events later!
                        this.snapshot = event.apply(this.snapshot);
                        this.snapshot.save(connection);
                    }
                }
                connection.commit();
                return !Objects.equals(this.snapshot, snapshotBefore);
            }
        } catch (SQLException | IOException e) {
            LOG.error("An error occurred when restoring from database!", e);
            this.snapshot = oldSnapshot;
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e2) {
                LOG.error("Error in rollback after previous error", e2);
            }
            return false;
        }
    }

    private void registerInitialTags() {
        Snapshot oldSnapshot = this.snapshot;
        Connection connection = null;
        try {
            connection = Database.createConnection(EnumSet.of(READ_WRITE));
            for (Team team : Config.getCurrentConfig().getTeams()) {
                for (TagId tag : team.getTags()) {
                    AddTagEvent e = new AddTagEvent(Instant.EPOCH, tag, team.getTeamNb());
                    e.save(connection);
                    this.snapshot = e.apply(this.snapshot);
                    this.snapshot.save(connection);
                }
            }
            connection.commit();
        } catch (SQLException e) {
            LOG.error("An error occurred when registering initial tags!", e);
            this.snapshot = oldSnapshot;
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e2) {
                LOG.error("Error in rollback after previous error", e2);
            }
        }
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
                processEvent(event);
                // TODO(Roel): Do deferred event processing later!
            }
        } catch (InterruptedException ignored) {
        }
        LOG.info("Stopping processor!");
        executorService.shutdownNow();
        commandProcessor.stop();
        readerListeners.stream().forEach(listener -> {
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

    private Optional<Long> getLastUpdateForReader(Connection connection, int readerId) throws SQLException {
        String statement = "SELECT \"updateCount\" FROM \"tagSeenEvents\" WHERE \"readerId\" = ? " +
                "ORDER BY \"updateCount\" DESC FETCH FIRST ROW ONLY";
        try (PreparedStatement stmt = connection.prepareStatement(statement)) {
            stmt.setInt(1, readerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getLong("updateCount"));
            } else {
                return Optional.empty();
            }
        }
    }

    private void trySpawnReaderListener(int readerId) {
        URI uri = Config.getCurrentConfig().getReader(readerId).getURI();
        String updateChannel = JedisHelper.dbLocalChannel(Config.getCurrentConfig().getUpdateChannel(), uri);
        try {
            Optional<Long> lastUpdate;
            {
                Connection connection = null;
                try {
                    connection = Database.createConnection(EnumSet.of(READ_ONLY));
                    lastUpdate = getLastUpdateForReader(connection, readerId);
                    connection.commit();
                } catch (SQLException e) {
                    if (connection != null) connection.rollback();
                    throw e;
                }
            }
            Jedis subJedis = JedisHelper.get(uri);
            ReaderListener listener = new ReaderListener(readerId, this::queueEvent, lastUpdate);
            readerListeners.add(listener);
            Thread thread = new Thread(() -> {
                try {
                    LOG.info("Reader listener {} subscribing to channel {} on uri {}", readerId, updateChannel, uri);
                    subJedis.subscribe(listener, updateChannel);
                } catch (JedisConnectionException e) {
                    LOG.error("Reader listener {} stopped (uri: {}) because of a connection failure, scheduling reconnect",
                            readerId, uri, e);
                    executorService.schedule(() -> trySpawnReaderListener(readerId), 5L, TimeUnit.SECONDS);
                }
            });
            threads.add(thread);
            thread.start();
        } catch (SQLException e) {
            LOG.error("Error fetching last update for reader {} from database, scheduling retry", readerId, e);
            executorService.schedule(() -> trySpawnReaderListener(readerId), 5L, TimeUnit.SECONDS);
        } catch (JedisConnectionException e) {
            LOG.error("Couldn't connect to reader {}, uri {}, scheduling reconnect", readerId, uri, e);
            executorService.schedule(() -> trySpawnReaderListener(readerId), 5L, TimeUnit.SECONDS);
        }
    }

    private void spawnReaderListener(int readerId) {
        executorService.submit(() -> trySpawnReaderListener(readerId));
    }

    private void processEvent(Event event) {
        logProcessEvent(event);
        Connection connection = null;
        Snapshot oldSnapshot = this.snapshot;
        try {
            connection = Database.createConnection(EnumSet.of(READ_WRITE));
            Event firstEvent = event;
            if (event.isUnique()) {
                Optional<Event> other = Event.loadUnique(connection, event.getClass());
                if (other.isPresent()) {
                    other.get().setRemoved(connection, true);
                    if (!other.get().getTime().isAfter(event.getTime())) {
                        firstEvent = other.get();
                    }
                }
            }
            event.save(connection);
            Snapshot snapshotToUpdateFrom = this.snapshot;
            if (!firstEvent.getTime().isAfter(this.snapshot.getSnapshotTime())) {
                LOG.debug("Event before current snapshot, loading snapshot before");
                Optional<Snapshot> s = Snapshot.loadBefore(connection, firstEvent.getTime());
                if (s.isPresent()) snapshotToUpdateFrom = s.get();
                else snapshotToUpdateFrom = new Snapshot(Instant.EPOCH);
            }
            List<Event> events;
            Snapshot.deleteAfter(connection, snapshotToUpdateFrom);
            LOG.debug("Updating from snapshot: {}", snapshotToUpdateFrom.getId());
            if (snapshotToUpdateFrom.getId().isPresent()) {
                assert snapshotToUpdateFrom.getEventId().isPresent();
                events = Event.loadAfter(connection, snapshotToUpdateFrom.getSnapshotTime(), snapshotToUpdateFrom.getEventId().get());
            } else {
                events = Event.loadAll(connection);
            }
            for (Event e : events) {
                if (!e.isRemoved()) {
                    snapshotToUpdateFrom = e.apply(snapshotToUpdateFrom);
                    snapshotToUpdateFrom.save(connection);
                }
            }
            LOG.debug("Updated up to snapshot: {}", snapshotToUpdateFrom.getId());
            this.snapshot = snapshotToUpdateFrom;
            connection.commit();
            // TODO: Provide a sensible message for NEW_SNAPSHOT?
            statusReporter.broadcast(new StatusMessage(StatusMessage.MessageType.NEW_SNAPSHOT, "New snapshot!"));
            if (eventCallbacks.containsKey(event)) {
                eventCallbacks.get(event).accept(true);
                eventCallbacks.remove(event);
            }
        } catch (SQLException | IOException e) {
            LOG.error("Error when handling event!", e);
            this.snapshot = oldSnapshot;
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException e2) {
                LOG.error("Error in rollback after previous error!", e2);
            }
            // TODO(Roel): Reschedule event!
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
