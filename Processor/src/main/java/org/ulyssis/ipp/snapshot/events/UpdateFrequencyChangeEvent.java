package org.ulyssis.ipp.snapshot.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.control.commands.SetUpdateFrequencyCommand;
import org.ulyssis.ipp.snapshot.Snapshot;

import java.time.Instant;

@JsonTypeName("UpdateFrequencyChange")
public final class UpdateFrequencyChangeEvent extends Event {
    private final int updateFrequency;

    @JsonCreator
    public UpdateFrequencyChangeEvent(Instant time, int updateFrequency) {
        super(time);
        this.updateFrequency = updateFrequency;
    }

    @Override
    public Snapshot apply(Snapshot before) {
        return Snapshot.builder(getTime()).fromSnapshot(before).withUpdateFrequency(updateFrequency).build();
    }

    public int getUpdateFrequency() {
        return updateFrequency;
    }

    public static UpdateFrequencyChangeEvent fromCommand(Command command) {
        assert(command instanceof SetUpdateFrequencyCommand);
        SetUpdateFrequencyCommand cmd = (SetUpdateFrequencyCommand) command;
        return new UpdateFrequencyChangeEvent(cmd.getTime(), cmd.getUpdateFrequency());
    }
}
