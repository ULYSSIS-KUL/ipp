package org.ulyssis.ipp.control.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.updates.Status;

import java.time.Instant;

@JsonTypeName("SetStatus")
public final class SetStatusCommand extends Command {
    private final Status status;

    public SetStatusCommand(Status status) {
        super();
        this.status = status;
    }

    @JsonCreator
    private SetStatusCommand(@JsonProperty("time") Instant time,
                             @JsonProperty("status") Status status) {
        super(time);
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
