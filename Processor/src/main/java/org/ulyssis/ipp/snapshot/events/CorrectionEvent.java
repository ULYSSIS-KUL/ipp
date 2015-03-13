package org.ulyssis.ipp.snapshot.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.control.commands.CorrectionCommand;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.snapshot.TeamState;
import org.ulyssis.ipp.snapshot.TeamStates;

import java.time.Instant;
import java.util.Optional;

@JsonTypeName("Correction")
public final class CorrectionEvent extends Event {
    private int teamNb;
    private int correction;

    /**
     * Constructor for Jackson
     */
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

    public Snapshot apply(Snapshot snapshot) {
        TeamStates oldTeamStates = snapshot.getTeamStates();
        Optional<TeamState> oldTeamState = oldTeamStates.getStateForTeam(teamNb);
        TeamState newTeamState;
        if (oldTeamState.isPresent()) {
            newTeamState = oldTeamState.get().addCorrection(correction);
        } else {
            newTeamState = new TeamState().addCorrection(correction);
        }
        return Snapshot.builder(getTime())
                .fromSnapshot(snapshot)
                .withTeamStates(snapshot.getTeamStates().setStateForTeam(teamNb, newTeamState))
                .build();
    }

    public static CorrectionEvent fromCommand(Command command) {
        assert(command instanceof CorrectionCommand);
        CorrectionCommand cmd = (CorrectionCommand) command;
        return new CorrectionEvent(cmd.getTime(), cmd.getTeamNb(), cmd.getCorrection());
    }
}
