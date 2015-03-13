package org.ulyssis.ipp.snapshot.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.control.commands.AddTagCommand;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.snapshot.TeamTagMap;
import org.ulyssis.ipp.TagId;

import java.time.Instant;

@JsonTypeName("AddTag")
public final class AddTagEvent extends TagEvent {
    @JsonCreator
    public AddTagEvent(
            @JsonProperty("time") Instant time,
            @JsonProperty("tag") TagId tag,
            @JsonProperty("teamNb") int teamNb) {
        super(time, tag, teamNb);
    }

    public Snapshot apply(Snapshot snapshot) {
        TeamTagMap newTeamTagMap = snapshot.getTeamTagMap().addTagToTeam(getTag(), getTeamNb());
        return Snapshot.builder(getTime()).fromSnapshot(snapshot).withTeamTagMap(newTeamTagMap).build();
    }

    public static AddTagEvent fromCommand(Command command) {
        assert(command instanceof AddTagCommand);
        AddTagCommand addTagCommand = (AddTagCommand) command;
        return new AddTagEvent(
            addTagCommand.getTime(),
            addTagCommand.getTag(),
            addTagCommand.getTeamNb()
        );
    }
}
