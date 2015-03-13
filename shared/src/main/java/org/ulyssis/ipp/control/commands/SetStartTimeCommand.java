package org.ulyssis.ipp.control.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.Instant;

@JsonTypeName("SetStartTime")
public final class SetStartTimeCommand extends Command {
    public SetStartTimeCommand() {
        super();
    }

    public SetStartTimeCommand(Instant startTime) {
        super(startTime);
    }

    @JsonCreator
    private SetStartTimeCommand(@JsonProperty("commandId") String commandId,
                                @JsonProperty("time") Instant time) {
        super(commandId, time);
    }
}
