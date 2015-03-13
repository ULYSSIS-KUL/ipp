package org.ulyssis.ipp.publisher;

import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.snapshot.TeamState;
import org.ulyssis.ipp.snapshot.TeamStates;
import org.ulyssis.ipp.snapshot.events.TagSeenEvent;
import org.ulyssis.ipp.updates.Status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

public final class Score {
    private final long time; // Time
    private final double lap; // Lap length
    private final int update; // Update frequency in ms
    private final Status status;
    private final String message;
    private final SortedSet<Team> teams; // The teams, sorted by score

    @JsonIgnoreProperties({"nonLimitedPosition"})
    public final class Team implements Comparable<Team> {
        private final int nb; // The team number
        private final String name; // The name of the team
        private final int laps; // The number of laps
        private final double position; // The predicted position of the team
        private final double speed; // The predicted speed of the team

        private final double nonLimitedPosition; // The predicted position, not limited

        public Team(int nb, String name, int laps, double position, double nonLimitedPosition, double speed) {
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

        public double getPosition() {
            return position;
        }
        
        public double getNonLimitedPosition() {
        	return nonLimitedPosition;
        }

        public double getSpeed() {
            return speed;
        }

        @Override
        public int compareTo(Team other) {
        	if (this.laps < other.laps) {
        		return 1;
        	} else if (this.laps > other.laps) {
        		return -1;
        	}
            double thisDistance = (this.laps + nonLimitedPosition) * Score.this.lap;
            double otherDistance = (other.laps + other.nonLimitedPosition) * Score.this.lap;
            if (thisDistance < otherDistance) {
                return 1;
            } else if (thisDistance > otherDistance) {
                return -1;
            } else {
                return Integer.compare(this.nb, other.nb);
            }
        }
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
                    teams.add(new Team(team.getTeamNb(), team.getName(), 0, 0, 0, 0));
                } else {
                    TagSeenEvent lastEvent = t.getLastTagSeenEvent().get();
                    Instant lastTime = t.getLastTagSeenEvent().get().getTime();
                    double elapsedSeconds = Duration.between(lastTime, now).toMillis() / 1000D;
                    double previousReaderPosition = config.getReader(lastEvent.getReaderId()).getPosition();
                    double nonLimitedPosition = previousReaderPosition + elapsedSeconds * speed;
                    double position = nonLimitedPosition;
                    if (position > config.getTrackLength()) position = config.getTrackLength();
                    teams.add(new Team(team.getTeamNb(), team.getName(), t.getNbLaps(),
                            position / config.getTrackLength(), nonLimitedPosition / config.getTrackLength(), speed));
                }
            } else {
                teams.add(new Team(team.getTeamNb(), team.getName(), 0, 0, 0, 0));
            }
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

    public SortedSet<Team> getTeams() {
        return teams;
    }
}
