package org.ulyssis.ipp.snapshot.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.ulyssis.ipp.snapshot.Snapshot;

import java.time.Instant;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({ @JsonSubTypes.Type(value=StartEvent.class),
                @JsonSubTypes.Type(value=EndEvent.class),
                @JsonSubTypes.Type(value=AddTagEvent.class),
                @JsonSubTypes.Type(value=RemoveTagEvent.class),
                @JsonSubTypes.Type(value=CorrectionEvent.class),
                @JsonSubTypes.Type(value=TagSeenEvent.class),
                @JsonSubTypes.Type(value=IdentityEvent.class),
                @JsonSubTypes.Type(value=MessageEvent.class),
                @JsonSubTypes.Type(value=StatusChangeEvent.class)})
public abstract class Event {
    private Instant time;

    protected Event(Instant time) {
        this.time = time;
    }

    public Instant getTime() {
        return time;
    }

    /**
     * Determines whether this event should be unique, defaults to false
     *
     * @return whether this event should be unique (default implementation = false)
     */
    public boolean unique() {
        return false;
    }

    /**
     * Apply this event to a snapshot, yielding the new snapshot
     */
    public abstract Snapshot apply(Snapshot before);
}
