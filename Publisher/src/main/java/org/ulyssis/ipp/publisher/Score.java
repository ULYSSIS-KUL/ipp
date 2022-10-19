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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.snapshot.TeamState;
import org.ulyssis.ipp.snapshot.TeamStates;
import org.ulyssis.ipp.snapshot.TagSeenEvent;
import org.ulyssis.ipp.updates.Status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class Score {
    private final long time; // Time
    private final double lap; // Lap length
    private final int update; // Update frequency in ms
    private final Status status;
    private final String message;
    private final Collection<Team> teams; // The teams, sorted by score

    @JsonIgnoreProperties({"nonLimitedPosition","lap"})
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public static final class Team implements Comparable<Team> {
        private final double lap;
        private final int nb; // The team number
        private final String name; // The name of the team
        private final int laps; // The number of laps
        private final OptionalDouble position; // The predicted position of the team, not present if FinalScore or FinalHour
        private final OptionalDouble speed; // The predicted speed of the team, not present if FinalScore or FinalHour

        private final double nonLimitedPosition; // The predicted position, not limited

        @JsonCreator
        public Team(@JsonProperty("nb") int nb,
                    @JsonProperty("name") String name,
                    @JsonProperty("laps") int laps,
                    @JsonProperty("position") OptionalDouble position,
                    @JsonProperty("speed") OptionalDouble speed) {
            this(Double.NaN, nb, name, laps, position, Double.NaN, speed);
        }

        public Team(double lap, int nb, String name, int laps, OptionalDouble position, double nonLimitedPosition, OptionalDouble speed) {
            this.lap = lap;
            this.nb = nb;
            this.name = name;
            this.laps = laps;
            this.position = position;
            this.nonLimitedPosition = nonLimitedPosition;
            this.speed = speed;
        }

        public int getNb() {
            return nb;
        }

        public String getName() {
            return name;
        }

        public int getLaps() {
            return laps;
        }

        public OptionalDouble getPosition() {
            return position;
        }
        
        public double getNonLimitedPosition() {
        	return nonLimitedPosition;
        }

        public OptionalDouble getSpeed() {
            return speed;
        }

        @Override
        public int compareTo(Team other) {
        	if (this.laps < other.laps) {
        		return 1;
        	} else if (this.laps > other.laps) {
        		return -1;
        	}
            double thisDistance = (this.laps + nonLimitedPosition) * lap;
            double otherDistance = (other.laps + other.nonLimitedPosition) * lap;
            if (thisDistance < otherDistance) {
                return 1;
            } else if (thisDistance > otherDistance) {
                return -1;
            } else {
                return Integer.compare(this.nb, other.nb);
            }
        }
    }

    @JsonCreator
    public Score(@JsonProperty("time") long time,
                 @JsonProperty("lap") double lap,
                 @JsonProperty("update") int update,
                 @JsonProperty("status") Status status,
                 @JsonProperty("message") String message,
                 @JsonProperty("teams") List<Team> teams) {
        this.time = time;
        this.lap = lap;
        this.update = update;
        this.status = status;
        this.message = message;
        this.teams = teams;
    }

    public Score(Snapshot snapshot) {
    	this(snapshot, true);
    }

    public Score(Snapshot snapshot, boolean publicScore) {
        Config config = Config.getCurrentConfig();
        Instant now = Instant.now();
        this.time = now.toEpochMilli();
        this.lap = config.getTrackLength();
        this.update = snapshot.getUpdateFrequency();
        this.teams = new TreeSet<>();
        this.status = snapshot.getStatus();
        this.message = snapshot.getStatusMessage();
        TeamStates teamStates = publicScore ? snapshot.getPublicTeamStates() : snapshot.getTeamStates();
        for (org.ulyssis.ipp.config.Team team : config.getTeams()) {
            Optional<TeamState> teamState = teamStates.getStateForTeam(team.getTeamNb());
            if (teamState.isPresent()) {
                TeamState t = teamState.get();
                double speed = t.getPredictedSpeed();
                if (Double.isNaN(speed)) {
                    teams.add(new Team(lap, team.getTeamNb(), team.getName(), 0,
                            doubleOrEmpty(this.status, 0),
                            0,
                            doubleOrEmpty(this.status, 0)));
                } else {
                    TagSeenEvent lastEvent = t.getLastTagSeenEvent().get();
                    Instant lastTime = t.getLastTagSeenEvent().get().getTime();
                    double elapsedSeconds = Duration.between(lastTime, now).toMillis() / 1000D;
                    double previousReaderPosition = config.getReader(lastEvent.getReaderId()).getPosition();
                    double nonLimitedPosition = previousReaderPosition + elapsedSeconds * speed;
                    double position = nonLimitedPosition;
                    if (position > config.getTrackLength()) position = config.getTrackLength();
                    teams.add(new Team(lap, team.getTeamNb(), team.getName(), t.getNbLaps(),
                            doubleOrEmpty(this.status, position / config.getTrackLength()),
                            nonLimitedPosition / config.getTrackLength(),
                            doubleOrEmpty(this.status, speed)));
                }
            } else {
                teams.add(new Team(lap, team.getTeamNb(), team.getName(), 0,
                        doubleOrEmpty(this.status,0),
                        0,
                        doubleOrEmpty(this.status,0)));
            }
        }
    }

    private static OptionalDouble doubleOrEmpty(Status status, double d) {
        if (status == Status.FinalScore || status == Status.FinalHour) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(d);
        }
    }

    public long getTime() {
        return time;
    }

    public double getLap() {
        return lap;
    }

    public int getUpdate() {
        return update;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Collection<Team> getTeams() {
        return teams;
    }
}
