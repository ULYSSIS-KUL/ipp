package org.ulyssis.ipp.snapshot.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.control.commands.RemoveTagCommand;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.snapshot.TeamTagMap;
import org.ulyssis.ipp.TagId;

import java.time.Instant;

@JsonTypeName("RemoveTag")
public final class RemoveTagEvent extends TagEvent {
    @JsonCreator
    public RemoveTagEvent(
            @JsonProperty("time") Instant time,
            @JsonProperty("tag") TagId tag,
            @JsonProperty("teamNb") int teamNb) {
        super(time, tag, teamNb);
    }

    public Snapshot apply(Snapshot snapshot) {
        TeamTagMap newTeamTagMap = snapshot.getTeamTagMap().removeTag(getTag());
        return Snapshot.builder(getTime()).fromSnapshot(snapshot).withTeamTagMap(newTeamTagMap).build();
    }

    public static RemoveTagEvent fromCommand(Command command) {
        assert(command instanceof RemoveTagCommand);
        RemoveTagCommand cmd = (RemoveTagCommand) command;
        return new RemoveTagEvent(cmd.getTime(), cmd.getTag(), cmd.getTeamNb());
    }
}
