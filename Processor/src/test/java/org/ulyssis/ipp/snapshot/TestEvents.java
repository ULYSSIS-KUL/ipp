package org.ulyssis.ipp.snapshot;

import org.junit.Test;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.control.commands.SetStatusMessageCommand;
import org.ulyssis.ipp.utils.Serialization;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestEvents {
    @Test
    public void testSerializeMessageCommand() throws Exception {
        String msg = "My status message";
        SetStatusMessageCommand cmd = new SetStatusMessageCommand(Instant.EPOCH, msg);
        byte[] b = Serialization.getJsonMapper().writeValueAsBytes(cmd);
        SetStatusMessageCommand cmdAfter = (SetStatusMessageCommand)Serialization.getJsonMapper().readValue(b, Command.class);
        assertThat(cmdAfter.getMessage(), equalTo(msg));
        assertThat(cmdAfter.getTime(), equalTo(Instant.EPOCH));
    }

    @Test
    public void testSerializeMessageEvent() throws Exception {
        String msg = "My status message";
        MessageEvent event = new MessageEvent(Instant.EPOCH, msg);
        byte[] b = Serialization.getJsonMapper().writeValueAsBytes(event);
        MessageEvent eventAfter = (MessageEvent)Serialization.getJsonMapper().readValue(b, Event.class);
        assertThat(eventAfter.getMessage(), equalTo(msg));
        assertThat(eventAfter.getTime(), equalTo(Instant.EPOCH));
    }

    @Test
    public void testMessageEventFromCommand() throws Exception {
        String msg = "My status message";
        SetStatusMessageCommand cmd = new SetStatusMessageCommand(Instant.EPOCH, msg);
        MessageEvent event = MessageEvent.fromCommand(cmd);
        assertThat(event.getMessage(), equalTo(cmd.getMessage()));
        assertThat(event.getTime(), equalTo(cmd.getTime()));
        assertThat(event.getMessage(), equalTo(msg));
        assertThat(event.getTime(), equalTo(Instant.EPOCH));
    }
}
