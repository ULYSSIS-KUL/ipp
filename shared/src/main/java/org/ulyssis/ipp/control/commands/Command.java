package org.ulyssis.ipp.control.commands;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({ @JsonSubTypes.Type(value=AddTagCommand.class),
        @JsonSubTypes.Type(value=CorrectionCommand.class),
        @JsonSubTypes.Type(value=PingCommand.class),
        @JsonSubTypes.Type(value=RemoveTagCommand.class),
        @JsonSubTypes.Type(value=RestartCommand.class),
        @JsonSubTypes.Type(value=SetEndTimeCommand.class),
        @JsonSubTypes.Type(value=SetStartTimeCommand.class),
        @JsonSubTypes.Type(value=SetStatusMessageCommand.class),
        @JsonSubTypes.Type(value=SetStatusCommand.class),
        @JsonSubTypes.Type(value=SetUpdateFrequencyCommand.class) })
public abstract class Command {
    private final String commandId;
    private final Instant time;

    public Command() {
        this.commandId = generateCommandId();
        this.time = Instant.now();
    }

    public Command(Instant time) {
        this.commandId = generateCommandId();
        this.time = time;
    }

    public Command(String commandId, Instant time) {
        this.commandId = commandId;
        this.time = time;
    }

    public static String generateCommandId() {
        return UUID.randomUUID().toString();
    }

    public String getCommandId() {
        return commandId;
    }

    public Instant getTime() {
        return time;
    }
}
