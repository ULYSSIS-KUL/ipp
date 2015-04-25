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
package org.ulyssis.ipp.snapshot.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.snapshot.TeamState;
import org.ulyssis.ipp.snapshot.TeamStates;
import org.ulyssis.ipp.updates.TagUpdate;
import org.ulyssis.ipp.TagId;

import java.time.Instant;
import java.util.Optional;

@JsonTypeName("TagSeen")
public final class TagSeenEvent extends Event {
    private static final Logger LOG = LogManager.getLogger(TagSeenEvent.class);

    private TagId tag;
    private int readerId;

    @JsonCreator
    public TagSeenEvent(
            @JsonProperty("time") Instant time,
            @JsonProperty("tag") TagId tag,
            @JsonProperty("readerId") int readerId) {
        super(time);
        this.tag = tag;
        this.readerId = readerId;
    }

    public TagSeenEvent(TagUpdate update) {
        this(update.getUpdateTime(), update.getTag(), update.getReaderId());
    }

    public TagId getTag() {
        return tag;
    }

    private void setTag(TagId tag) {
        this.tag = tag;
    }

    public int getReaderId() {
        return readerId;
    }

    public Snapshot apply(Snapshot snapshot) {
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
                getTime().equals(otherEvent.getTime());
    }

    @Override
    public int hashCode() {
        return tag.toString().hashCode() ^ readerId ^ getTime().hashCode();
    }
}
