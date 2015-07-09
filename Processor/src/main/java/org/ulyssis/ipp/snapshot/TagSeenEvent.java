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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.updates.TagUpdate;
import org.ulyssis.ipp.TagId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

@JsonTypeName("TagSeen")
public final class TagSeenEvent extends Event {
    private static final Logger LOG = LogManager.getLogger(TagSeenEvent.class);

    private TagId tag;
    private int readerId;
    private long updateCount;

    @JsonCreator
    public TagSeenEvent(
            @JsonProperty("time") Instant time,
            @JsonProperty("tag") TagId tag,
            @JsonProperty("readerId") int readerId,
            @JsonProperty("updateCount") long updateCount) {
        super(time);
        this.tag = tag;
        this.readerId = readerId;
        this.updateCount = updateCount;
    }

    public TagSeenEvent(TagUpdate update) {
        this(update.getUpdateTime(), update.getTag(), update.getReaderId(), update.getUpdateCount());
    }

    public TagId getTag() {
        return tag;
    }

    public int getReaderId() {
        return readerId;
    }

    protected Snapshot doApply(Snapshot snapshot) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Applying TagSeenEvent for tag {}, reader id {}", tag, readerId);
        }
        if (snapshot.getStartTime().isBefore(getTime()) &&
                snapshot.getEndTime().isAfter(getTime())) {
            Optional<Integer> teamNb = snapshot.getTeamTagMap().tagToTeam(tag);
            if (teamNb.isPresent()) {
                Optional<TeamState> teamState = snapshot.getTeamStates().getStateForTeam(teamNb.get());
                TeamState newTeamState;
                if (teamState.isPresent()) {
                    newTeamState = teamState.get().addTagSeenEvent(snapshot, this);
                } else {
                    newTeamState = (new TeamState()).addTagSeenEvent(snapshot, this);
                }
                TeamStates newTeamStates = snapshot.getTeamStates().setStateForTeam(teamNb.get(), newTeamState);
                Snapshot.Builder builder = Snapshot.builder(getTime())
                        .fromSnapshot(snapshot)
                        .withTeamStates(newTeamStates);
                if (snapshot.getStatus().isPublic()) {
                    builder.withPublicTeamStates(newTeamStates);
                }
                return builder.build();
            } else {
                return snapshot;
            }
        } else {
            return snapshot;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (!(other instanceof TagSeenEvent)) return false;
        if (this == other) return true;
        TagSeenEvent otherEvent = (TagSeenEvent) other;
        return tag.equals(otherEvent.tag) &&
                readerId == otherEvent.readerId &&
                getTime().equals(otherEvent.getTime()) &&
                updateCount == otherEvent.updateCount;
    }

    @Override
    public int hashCode() {
        return tag.toString().hashCode() ^ readerId ^ getTime().hashCode() ^ Long.hashCode(updateCount);
    }

    @Override
    public void save(Connection connection) throws SQLException {
        super.save(connection);
        String statement = "INSERT INTO \"tagSeenEvents\" (\"id\",\"readerId\",\"updateCount\") VALUES (?,?,?)";
        try (PreparedStatement stmt = connection.prepareStatement(statement)) {
            stmt.setLong(1, getId().get());
            stmt.setInt(2, readerId);
            stmt.setLong(3, updateCount);
            boolean result = stmt.execute();
            assert !result;
        }
    }
}
