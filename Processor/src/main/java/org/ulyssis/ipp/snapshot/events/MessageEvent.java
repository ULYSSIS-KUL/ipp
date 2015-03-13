package org.ulyssis.ipp.snapshot.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.control.commands.SetStatusMessageCommand;
import org.ulyssis.ipp.snapshot.Snapshot;

import java.time.Instant;

@JsonTypeName("Message")
public final class MessageEvent extends Event {
    private final String message;

    @JsonCreator
    public MessageEvent(
            @JsonProperty("time") Instant time,
            @JsonProperty("message") String message) {
        super(time);
        this.message = message;
    }

    @Override
    public Snapshot apply(Snapshot before) {
        return Snapshot.builder(getTime()).fromSnapshot(before).withStatusMessage(message).build();
    }

    public static MessageEvent fromCommand(Command command) {
        assert(command instanceof SetStatusMessageCommand);
        SetStatusMessageCommand cmd = (SetStatusMessageCommand) command;
        return new MessageEvent(cmd.getTime(), cmd.getMessage());
    }
}
