package org.ulyssis.ipp.control.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.TagId;

import java.time.Instant;

/**
 * The "add tag" command, for adding a tag id to a team number.
 */
@JsonTypeName("AddTag")
public final class AddTagCommand extends TagCommand {

    /**
     * Create an AddTagCommand for the given tag and team number.
     *
     * The time is set to be the current time.
     *
     * @param tag
     *        The tag to add.
     * @param teamNb
     *        The team number to add the tag for.
     */
    public AddTagCommand(TagId tag, int teamNb) {
        super(tag, teamNb);
    }

    /**
     * Create an AddTagCommand for the given tag and team number at the given time.
     *
     * @param time
     *        The time when to add the tag, this can be in the future for
     *        an anticipated adding of a tag, or in the past, to add a tag
     *        afterwards (e.g. as a correction)
     * @param tag
     *        The tag to add.
     * @param teamNb
     *        The team number to add the tag for.
     */
    public AddTagCommand(Instant time, TagId tag, int teamNb) {
        super(time, tag, teamNb);
    }

    /*
     * Only for deserialization.
     */
    @JsonCreator
    private AddTagCommand(@JsonProperty("commandId") String commandId,
                          @JsonProperty("time") Instant time,
                          @JsonProperty("tag") TagId tag,
                          @JsonProperty("teamNb") int teamNb) {
        super(commandId, time, tag, teamNb);
    }
}
