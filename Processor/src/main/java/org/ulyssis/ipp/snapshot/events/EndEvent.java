package org.ulyssis.ipp.snapshot.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.control.commands.SetEndTimeCommand;
import org.ulyssis.ipp.snapshot.Snapshot;

import java.time.Instant;

@JsonTypeName("End")
public final class EndEvent extends Event {
    /**
     * Constructor for Jackson
     */
    private EndEvent() {
        super(Instant.MIN);
    }

    public EndEvent(Instant time) {
        super(time);
    }

    public Snapshot apply(Snapshot snapshot) {
        return Snapshot.builder(getTime()).fromSnapshot(snapshot).withEndTime(getTime()).build();
    }

    public static EndEvent fromCommand(Command command) {
        assert(command instanceof SetEndTimeCommand);
        SetEndTimeCommand cmd = (SetEndTimeCommand) command;
        return new EndEvent(cmd.getTime());
    }

    @Override
    public boolean unique() {
        return true;
    }
}
