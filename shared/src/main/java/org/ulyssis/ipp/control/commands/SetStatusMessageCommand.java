package org.ulyssis.ipp.control.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.Instant;

@JsonTypeName("SetStatusMessage")
public class SetStatusMessageCommand extends Command {
    private final String message;

    public SetStatusMessageCommand(String message) {
        super();
        this.message = message;
    }

    public SetStatusMessageCommand(Instant time, String message) {
        super(time);
        this.message = message;
    }

    @JsonCreator
    private SetStatusMessageCommand(@JsonProperty("commandId") String commandId,
                                    @JsonProperty("time") Instant time,
                                    @JsonProperty("message") String message) {
        super(commandId, time);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
