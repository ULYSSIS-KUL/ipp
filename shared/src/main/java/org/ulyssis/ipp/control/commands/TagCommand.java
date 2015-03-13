package org.ulyssis.ipp.control.commands;

import org.ulyssis.ipp.TagId;

import java.time.Instant;

public abstract class TagCommand extends Command {
    private final TagId tag;
    private final int teamNb;

    public TagCommand(TagId tag, int teamNb) {
        super();
        this.tag = tag;
        this.teamNb = teamNb;
    }

    public TagCommand(Instant time, TagId tag, int teamNb) {
        super(time);
        this.tag = tag;
        this.teamNb = teamNb;
    }

    public TagCommand(String commandId, Instant time, TagId tag, int teamNb) {
        super(commandId, time);
        this.tag = tag;
        this.teamNb = teamNb;
    }

    public TagId getTag() {
        return tag;
    }

    public int getTeamNb() {
        return teamNb;
    }
}
