package org.ulyssis.ipp.control.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.Instant;

@JsonTypeName("Restart")
public final class RestartCommand extends Command {
    public RestartCommand() {
        super();
    }

    @JsonCreator
    private RestartCommand(@JsonProperty("commandId") String commandId,
                           @JsonProperty("time") Instant time) {
        super(commandId, time);
    }
}
