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

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.control.commands.CorrectionCommand;

import java.time.Instant;
import java.util.Optional;

@JsonTypeName("Correction")
public final class CorrectionEvent extends Event {
    private int teamNb;
    private int correction;

    /**
     * Constructor for Jackson
     */
    @SuppressWarnings("unused")
    private CorrectionEvent() {
        super(Instant.MIN);
    }

    /**
     * Create an event representing a correction of score.
     *
     * @param time
     *        The time at which the corection is performed.
     * @param teamNb
     *        The number of the team to correct the score of.
     * @param correction
     *        The number of laps that should be added (positive)
     *        or removed (negative)
     */
    public CorrectionEvent(Instant time, int teamNb, int correction) {
        super(time);
        this.teamNb = teamNb;
        this.correction = correction;
    }

    public int getTeamNb() {
        return teamNb;
    }

    public int getCorrection() {
        return correction;
    }

    protected Snapshot doApply(Snapshot snapshot) {
        TeamStates oldTeamStates = snapshot.getTeamStates();
        Optional<TeamState> oldTeamState = oldTeamStates.getStateForTeam(teamNb);
        TeamState newTeamState;
        if (oldTeamState.isPresent()) {
            newTeamState = oldTeamState.get().addCorrection(correction);
        } else {
            newTeamState = new TeamState().addCorrection(correction);
        }
        return Snapshot.builder(getTime(), snapshot)
                .withTeamStates(snapshot.getTeamStates().setStateForTeam(teamNb, newTeamState))
                .build();
    }

    public static CorrectionEvent fromCommand(Command command) {
        assert(command instanceof CorrectionCommand);
        CorrectionCommand cmd = (CorrectionCommand) command;
        return new CorrectionEvent(cmd.getTime(), cmd.getTeamNb(), cmd.getCorrection());
    }

    @Override
    public boolean isRemovable() {
        return true;
    }
}
