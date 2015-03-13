package org.ulyssis.ipp.control.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.Instant;

@JsonTypeName("Ping")
public final class PingCommand extends Command {
    public PingCommand() {
        super();
    }

    @JsonCreator
    private PingCommand(@JsonProperty("commandId") String commandId,
                        @JsonProperty("time") Instant time) {
        super(commandId, time);
    }
}
