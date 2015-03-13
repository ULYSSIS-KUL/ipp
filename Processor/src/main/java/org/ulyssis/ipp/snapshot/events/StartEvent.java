package org.ulyssis.ipp.snapshot.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.control.commands.SetStartTimeCommand;
import org.ulyssis.ipp.snapshot.Snapshot;

import java.time.Instant;

@JsonTypeName("Start")
public final class StartEvent extends Event {
    /**
     * Private constructor for Jackson
     */
    private StartEvent() {
        super(Instant.MIN);
    }

    public StartEvent(Instant time) {
        super(time);
    }

    public Snapshot apply(Snapshot snapshot) {
        return Snapshot.builder(getTime()).fromSnapshot(snapshot).withStartTime(getTime()).build();
    }

    public static StartEvent fromCommand(Command command) {
        assert(command instanceof SetStartTimeCommand);
        SetStartTimeCommand cmd = (SetStartTimeCommand) command;
        return new StartEvent(cmd.getTime());
    }

    @Override
    public boolean unique() {
        return true;
    }
}
