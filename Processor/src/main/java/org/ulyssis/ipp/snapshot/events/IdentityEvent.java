package org.ulyssis.ipp.snapshot.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.snapshot.Snapshot;

import java.time.Instant;

@JsonTypeName("Identity")
public final class IdentityEvent extends Event {
    @JsonCreator
    public IdentityEvent(
            @JsonProperty("time") Instant time) {
        super(time);
    }

    @Override
    public Snapshot apply(Snapshot before) {
        return Snapshot.builder(getTime()).fromSnapshot(before).build();
    }
}
