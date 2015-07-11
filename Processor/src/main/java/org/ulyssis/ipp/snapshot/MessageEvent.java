/*
 * Copyright (C) 2014-2015 ULYSSIS VZW
 *
 * This file is part of i++.
 * 
 * i++ is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Affero General Public License
 * as published by the Free Software Foundation. No other versions apply.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.ulyssis.ipp.snapshot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.control.commands.SetStatusMessageCommand;

import java.time.Instant;

@JsonTypeName("Message")
public final class MessageEvent extends Event {
    private final String message;

    @JsonCreator
    public MessageEvent(
            @JsonProperty("time") Instant time,
            @JsonProperty("message") String message) {
        super(time);
        this.message = message;
    }

    @Override
    protected Snapshot doApply(Snapshot before) {
        return Snapshot.builder(getTime(), before).withStatusMessage(message).build();
    }

    public static MessageEvent fromCommand(Command command) {
        assert(command instanceof SetStatusMessageCommand);
        SetStatusMessageCommand cmd = (SetStatusMessageCommand) command;
        return new MessageEvent(cmd.getTime(), cmd.getMessage());
    }

    @Override
    public boolean isRemovable() {
        return true;
    }
}
