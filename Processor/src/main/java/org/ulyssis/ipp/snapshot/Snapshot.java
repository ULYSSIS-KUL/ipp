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
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.updates.Status;
import org.ulyssis.ipp.utils.Serialization;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;

public final class Snapshot {
    private static final Logger LOG = LogManager.getLogger(Snapshot.class);

    public static class Builder {
        private Snapshot snapshot;

        private Builder(Instant time, Snapshot other) {
            snapshot = new Snapshot(time);
            if (other != null) {
                snapshot.teamTagMap = other.teamTagMap;
                snapshot.startTime = other.startTime;
                snapshot.endTime = other.endTime;
                snapshot.teamStates = other.teamStates;
                snapshot.publicTeamStates = other.publicTeamStates;
                snapshot.statusMessage = other.statusMessage;
                snapshot.status = other.status;
                snapshot.updateFrequency = other.updateFrequency;
            }
        }

        public Builder withTeamTagMap(TeamTagMap teamTagMap) {
            this.snapshot.teamTagMap = teamTagMap;
            return this;
        }

        public Builder withStartTime(Instant time) {
            this.snapshot.startTime = time;
            return this;
        }

        public Builder withEndTime(Instant time) {
            this.snapshot.endTime = time;
            return this;
        }

        public Builder withTeamStates(TeamStates teamStates) {
            this.snapshot.teamStates = teamStates;
            return this;
        }

        public Builder withPublicTeamStates(TeamStates teamStates) {
            this.snapshot.publicTeamStates = teamStates;
            return this;
        }

        public Builder withStatusMessage(String message) {
            this.snapshot.statusMessage = message;
            return this;
        }

        public Builder withStatus(Status status) {
            this.snapshot.status = status;
            return this;
        }

        public Builder withUpdateFrequency(int updateFrequency) {
            this.snapshot.updateFrequency = updateFrequency;
            return this;
        }

        public Snapshot build() {
            return snapshot;
        }
    }

    @JsonIgnore
    private long id = -1;

    @JsonIgnore
    long eventId = -1;

    public Optional<Long> getId() {
        if (id != -1) return Optional.of(id);
        else return Optional.empty();
    }

    public Optional<Long> getEventId() {
        if (eventId != -1) return Optional.of(eventId);
        else return Optional.empty();
    }

    /**
     * Default constructor for Jackson
     */
    @SuppressWarnings("unused")
    private Snapshot() {
    }

    public Snapshot(Instant time) {
        snapshotTime = time;
    }

    public Instant getSnapshotTime() {
        return snapshotTime;
    }

    public TeamTagMap getTeamTagMap() {
        return teamTagMap;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public TeamStates getTeamStates() {
        return teamStates;
    }

    public TeamStates getPublicTeamStates() {
        return publicTeamStates;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Status getStatus() {
        return status;
    }

    public int getUpdateFrequency() {
        return updateFrequency;
    }

    private Instant snapshotTime = Instant.MIN;
    private Instant startTime = Instant.MAX;
    private Instant endTime = Instant.MAX;
    private TeamTagMap teamTagMap = new TeamTagMap();
    private TeamStates teamStates;
    private TeamStates publicTeamStates;
    {
        teamStates = new TeamStates();
        publicTeamStates = teamStates;
    }
    private String statusMessage = "";
    private Status status = Status.NoResults;
    private int updateFrequency = 3;

    public static Builder builder(Instant time, Snapshot other) {
        return new Builder(time, other);
    }

    public static Optional<Snapshot> loadForEvent(Connection connection, Event event) throws SQLException, IOException {
        String statement = "SELECT \"id\", \"data\" FROM \"snapshots\" WHERE \"event\" = ?";
        try (PreparedStatement stmt = connection.prepareStatement(statement)) {
            stmt.setLong(1, event.getId().get());
            LOG.debug("executing query: {}", stmt);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String data = rs.getString("data");
                Snapshot result = Serialization.getJsonMapper().readValue(data, Snapshot.class);
                result.id = rs.getLong("id");
                result.eventId = event.getId().get();
                return Optional.of(result);
            } else {
                return Optional.empty();
            }
        }
    }

    public static Optional<Snapshot> loadLatest(Connection connection) throws SQLException, IOException {
        String statement = "SELECT \"id\", \"data\", \"event\" FROM \"snapshots\" ORDER BY \"time\" DESC FETCH FIRST ROW ONLY";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(statement)) {
            if (rs.next()) {
                String data = rs.getString("data");
                Snapshot result = Serialization.getJsonMapper().readValue(data, Snapshot.class);
                result.id = rs.getLong("id");
                result.eventId = rs.getLong("event");
                return Optional.of(result);
            } else {
                return Optional.empty();
            }
        }
    }

    public static Optional<Snapshot> loadBefore(Connection connection, Instant time) throws SQLException, IOException {
        String statement = "SELECT \"id\", \"data\", \"event\" FROM \"snapshots\" " +
                "WHERE \"time\" < ? ORDER BY \"time\" DESC, \"event\" DESC FETCH FIRST ROW ONLY";
        try (PreparedStatement stmt = connection.prepareStatement(statement)) {
            stmt.setTimestamp(1, Timestamp.from(time));
            LOG.debug("Executing query: {}", stmt);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String data = rs.getString("data");
                Snapshot result = Serialization.getJsonMapper().readValue(data, Snapshot.class);
                result.id = rs.getLong("id");
                result.eventId = rs.getLong("event");
                return Optional.of(result);
            } else {
                return Optional.empty();
            }
        }
    }

    public void save(Connection connection) throws SQLException {
        if (id != -1) return;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO \"snapshots\" (\"time\",\"data\",\"event\") VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setTimestamp(1, Timestamp.from(snapshotTime));
            String serialized;
            try {
                serialized = Serialization.getJsonMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                assert false; // TODO(Roel): Programming error
                return;
            }
            statement.setString(2, serialized);
            statement.setLong(3, eventId);
            statement.executeUpdate();
            ResultSet keys = statement.getGeneratedKeys();
            keys.next();
            this.id = keys.getLong(1);
        }
    }

    public static void deleteAfter(Connection connection, Snapshot snapshot) throws SQLException {
        String statement =
                "DELETE FROM \"snapshots\" WHERE \"time\" > ? OR (\"time\" = ? AND \"event\" > ?)";
        try (PreparedStatement stmt = connection.prepareStatement(statement)) {
            Timestamp timestamp = Timestamp.from(snapshot.getSnapshotTime());
            stmt.setTimestamp(1, timestamp);
            stmt.setTimestamp(2, timestamp);
            stmt.setLong(3, snapshot.getEventId().orElse(-1L));
            LOG.debug("Executing query: {}", stmt);
            int affectedRows = stmt.executeUpdate();
            LOG.debug("deleteAfter affected {} rows", affectedRows);
        }
    }
}
