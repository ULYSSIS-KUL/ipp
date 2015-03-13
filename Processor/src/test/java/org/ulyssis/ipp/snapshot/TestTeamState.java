package org.ulyssis.ipp.snapshot;

import org.junit.Test;
import org.ulyssis.ipp.snapshot.events.TagSeenEvent;
import org.ulyssis.ipp.utils.Serialization;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTeamState {
    @Test
    public void testSerializeToJson_DefaultObject() throws Exception {
        TeamState teamState = new TeamState();
        assertThat(Serialization.getJsonMapper().writeValueAsString(teamState), equalTo("{\"tagFragmentCount\":0}"));
    }

    @Test
    public void testDeserializeFromJson_DefaultObject() throws Exception {
        TeamState teamState = Serialization.getJsonMapper().readValue("{\"tagFragmentCount\":0}", TeamState.class);
        assertThat(teamState.getTagFragmentCount(), equalTo(0));
        assertThat(teamState.getLastTagSeenEvent(), equalTo(Optional.<TagSeenEvent>empty()));
    }
}
