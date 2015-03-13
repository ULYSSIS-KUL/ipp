package org.ulyssis.ipp.snapshot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;

import java.util.Optional;

public final class TeamStates {
    private final ImmutableMap<Integer, TeamState> teamNbToState;

    public TeamStates() {
        teamNbToState = ImmutableMap.of();
    }

    @JsonCreator
    private TeamStates(ImmutableMap<Integer, TeamState> teamNbToState) {
        this.teamNbToState = teamNbToState;
    }

    public Optional<TeamState> getStateForTeam(int teamNb) {
        if (teamNbToState.containsKey(teamNb)) {
            return Optional.of(teamNbToState.get(teamNb));
        } else {
            return Optional.empty();
        }
    }

    public TeamStates setStateForTeam(int teamNb, TeamState state) {
        ImmutableMap.Builder<Integer, TeamState> builder = ImmutableMap.builder();
        teamNbToState.forEach((myTeamNb, myState) -> {
            if (teamNb != myTeamNb) {
                builder.put(myTeamNb, myState);
            }
        });
        builder.put(teamNb, state);
        return new TeamStates(builder.build());
    }

    public int getNbLapsForTeam(int teamNb) {
        return getStateForTeam(teamNb).map(state -> state.getNbLaps()).orElse(0);
    }

    @JsonValue
    public ImmutableMap<Integer, TeamState> getTeamNbToState() {
        return teamNbToState;
    }
}
