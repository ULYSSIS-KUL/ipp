package org.ulyssis.ipp.control.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.Instant;

@JsonTypeName("SetEndTime")
public final class SetEndTimeCommand extends Command {
    public SetEndTimeCommand() {
        super();
    }

    public SetEndTimeCommand(Instant endTime) {
        super(endTime);
    }

    @JsonCreator
    private SetEndTimeCommand(@JsonProperty("commandId") String commandId,
                              @JsonProperty("time") Instant time) {
        super(commandId, time);
    }
}
