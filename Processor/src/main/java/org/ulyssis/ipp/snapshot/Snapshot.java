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

import org.ulyssis.ipp.updates.Status;

import java.time.Instant;

public final class Snapshot {
    public static class Builder {
        private Snapshot snapshot;

        private Builder(Instant time) {
            snapshot = new Snapshot();
            this.snapshot.snapshotTime = time;
        }

        public Builder fromSnapshot(Snapshot snapshot) {
            this.snapshot.teamTagMap = snapshot.teamTagMap;
            this.snapshot.startTime = snapshot.startTime;
            this.snapshot.endTime = snapshot.endTime;
            this.snapshot.teamStates = snapshot.teamStates;
            this.snapshot.publicTeamStates = snapshot.publicTeamStates;
            this.snapshot.statusMessage = snapshot.statusMessage;
            this.snapshot.status = snapshot.status;
            this.snapshot.updateFrequency = snapshot.updateFrequency;
            return this;
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

    private Snapshot() {
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

    public static Builder builder(Instant time) {
        return new Builder(time);
    }
}
