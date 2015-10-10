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
        return getStateForTeam(teamNb).map(TeamState::getNbLaps).orElse(0);
    }

    @JsonValue
    public ImmutableMap<Integer, TeamState> getTeamNbToState() {
        return teamNbToState;
    }
}
