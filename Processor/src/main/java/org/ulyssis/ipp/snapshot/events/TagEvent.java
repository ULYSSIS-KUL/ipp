package org.ulyssis.ipp.snapshot.events;

import org.ulyssis.ipp.TagId;

import java.time.Instant;

public abstract class TagEvent extends Event {
    private TagId tag;
    private int teamNb;

    public TagEvent(Instant instant, TagId tag, int teamNb) {
        super(instant);
        this.tag = tag;
        this.teamNb = teamNb;
    }

    public TagId getTag() {
        return tag;
    }

    private void setTag(TagId tag) {
        this.tag = tag;
    }

    public int getTeamNb() {
        return teamNb;
    }
}
