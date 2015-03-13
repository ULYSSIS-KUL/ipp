package org.ulyssis.ipp.control.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.TagId;

import java.time.Instant;

@JsonTypeName("RemoveTag")
public final class RemoveTagCommand extends TagCommand {
    public RemoveTagCommand(TagId tag, int teamNb) {
        super(tag, teamNb);
    }

    public RemoveTagCommand(Instant time, TagId tag, int teamNb) {
        super(time, tag, teamNb);
    }

    @JsonCreator
    private RemoveTagCommand(@JsonProperty("commandId") String commandId,
                             @JsonProperty("time") Instant time,
                             @JsonProperty("tag") TagId tag,
                             @JsonProperty("teamNb") int teamNb) {
        super(commandId, time, tag, teamNb);
    }
}
