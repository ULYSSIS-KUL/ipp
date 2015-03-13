package org.ulyssis.ipp.control.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.Instant;

@JsonTypeName("SetUpdateFrequency")
public final class SetUpdateFrequencyCommand extends Command {
    private final int updateFrequency;

    public SetUpdateFrequencyCommand(int updateFrequency) {
        super();
        this.updateFrequency = updateFrequency;
    }

    @JsonCreator
    private SetUpdateFrequencyCommand(Instant time, int updateFrequency) {
        super(time);
        this.updateFrequency = updateFrequency;
    }

    public int getUpdateFrequency() {
        return updateFrequency;
    }
}
