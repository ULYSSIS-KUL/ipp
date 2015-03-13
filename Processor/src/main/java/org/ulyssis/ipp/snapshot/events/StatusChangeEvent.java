package org.ulyssis.ipp.snapshot.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.control.commands.SetStatusCommand;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.updates.Status;

import java.time.Instant;

@JsonTypeName("StatusChange")
public final class StatusChangeEvent extends Event {
    private final Status status;

    @JsonCreator
    public StatusChangeEvent(@JsonProperty("time") Instant time,
                             @JsonProperty("status")Status status) {
        super(time);
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public static StatusChangeEvent fromCommand(Command command) {
        assert(command instanceof SetStatusCommand);
        SetStatusCommand cmd = (SetStatusCommand) command;
        return new StatusChangeEvent(cmd.getTime(), cmd.getStatus());
    }

    @Override
    public Snapshot apply(Snapshot before) {
        Snapshot.Builder builder = Snapshot.builder(getTime()).fromSnapshot(before).withStatus(status);
        if (!before.getStatus().isPublic() && status.isPublic()) {
            builder.withPublicTeamStates(before.getTeamStates());
        }
        return builder.build();
    }
}
