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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.ulyssis.ipp.utils.Serialization;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({ @JsonSubTypes.Type(value=StartEvent.class),
                @JsonSubTypes.Type(value=EndEvent.class),
                @JsonSubTypes.Type(value=AddTagEvent.class),
                @JsonSubTypes.Type(value=RemoveTagEvent.class),
                @JsonSubTypes.Type(value=CorrectionEvent.class),
                @JsonSubTypes.Type(value=TagSeenEvent.class),
                @JsonSubTypes.Type(value=IdentityEvent.class),
                @JsonSubTypes.Type(value=MessageEvent.class),
                @JsonSubTypes.Type(value=StatusChangeEvent.class)})
public abstract class Event {
    @JsonIgnore
    private long id = -1;
    @JsonIgnore
    private boolean removed = false;

    private Instant time;

    protected Event(Instant time) {
        this.time = time;
    }

    @JsonIgnore
    public final Optional<Long> getId() {
        if (id != -1) return Optional.of(id);
        else return Optional.empty();
    }

    @JsonIgnore
    public final boolean isRemoved() {
        if (id == -1) throw new IllegalStateException("Trying to request removed state of event not in db");
        return removed;
    }

    public final Instant getTime() {
        return time;
    }

    /**
     * Determines whether this event should be isUnique, defaults to false
     *
     * @return whether this event should be isUnique (default implementation = false)
     */
    @JsonIgnore
    public boolean isUnique() {
        return false;
    }

    /**
     * Determines whether this event can be removed or undone.
     *
     * @return whether this event can be removed or undone (default implementation = isUnique())
     */
    @JsonIgnore
    public boolean isRemovable() {
        return isUnique();
    }

    /**
     * Apply this event to a snapshot, yielding the new snapshot
     */
    protected abstract Snapshot doApply(Snapshot before);

    public final Snapshot apply(Snapshot before) {
        assert !removed;
        Snapshot result = doApply(before);
        result.eventId = this.id;
        return result;
    }

    // TODO: How to deal with deserialization problem?
    public static List<Event> loadAll(Connection connection) throws SQLException, IOException {
        String statement = "SELECT \"id\", \"data\", \"removed\" FROM \"events\" ORDER BY \"time\" ASC, \"id\" ASC";
        List<Event> events = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet result = stmt.executeQuery(statement)) {
            while (result.next()) {
                String evString = result.getString("data");
                Event event = Serialization.getJsonMapper().readValue(evString, Event.class);
                event.id = result.getLong("id");
                event.removed = result.getBoolean("removed");
                events.add(event);
            }
        }
        return events;
    }

    public static Optional<Event> loadUnique(Connection connection, Class<? extends Event> eventType) throws SQLException, IOException {
        String statement = "SELECT \"id\", \"data\" FROM \"events\" WHERE \"type\" = ? AND \"removed\" = false";
        try (PreparedStatement stmt = connection.prepareStatement(statement)) {
            stmt.setString(1, eventType.getSimpleName());
            ResultSet result = stmt.executeQuery();
            if (result.next()) {
                String evString = result.getString("data");
                Event event = Serialization.getJsonMapper().readValue(evString, Event.class);
                event.id = result.getLong("id");
                event.removed = false;
                return Optional.of(event);
            } else {
                return Optional.empty();
            }
        }
    }

    public static Optional<Event> load(Connection connection, long id) throws SQLException, IOException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "SELECT \"data\",\"removed\" FROM \"events\" WHERE \"id\"=?")) {
            statement.setLong(1, id);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                String evString = result.getString("data");
                Event event = Serialization.getJsonMapper().readValue(evString, Event.class);
                event.id = id;
                event.removed = result.getBoolean("removed");
                return Optional.of(event);
            } else {
                return Optional.empty();
            }
        }
    }

    public static List<Event> loadFrom(Connection connection, Instant time, long id) throws SQLException, IOException {
        String statement = "SELECT \"id\",\"data\",\"removed\" FROM \"events\" " +
                "WHERE \"time\" >= ? AND \"id\" >= ? ORDER BY \"time\" ASC, \"id\" ASC";
        List<Event> events = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(statement)) {
            stmt.setTimestamp(1, Timestamp.from(time));
            stmt.setLong(2, id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String evString = rs.getString("data");
                Event event = Serialization.getJsonMapper().readValue(evString, Event.class);
                event.id = rs.getLong("id");
                event.removed = rs.getBoolean("removed");
                events.add(event);
            }
        }
        return events;
    }

    public void save(Connection connection) throws SQLException {
        if (id != -1) return;
        try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO \"events\" (\"time\",\"type\",\"data\",\"removed\") " +
                            "VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setTimestamp(1, Timestamp.from(time));
            String serialized;
            try {
                serialized = Serialization.getJsonMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                assert false;
                throw new IllegalStateException(e); // TODO(Roel): is this appropriate?
            }
            statement.setString(2, this.getClass().getSimpleName());
            statement.setString(3, serialized);
            statement.setBoolean(4, removed);
            statement.executeUpdate();
            ResultSet keys = statement.getGeneratedKeys();
            keys.next();
            this.id = keys.getLong(1);
        }
    }

    public void setRemoved(Connection connection, boolean removed) throws SQLException {
        if (!isRemovable()) {
            assert false; // This is a programming error
            return;
        }
        PreparedStatement statement =
                connection.prepareStatement(
                        "UPDATE \"events\" SET \"removed\"=? WHERE \"id\"=?");
        statement.setBoolean(1, removed);
        statement.setLong(2, id);
        boolean result = statement.execute();
        assert(!result);
        this.removed = true;
    }
}
