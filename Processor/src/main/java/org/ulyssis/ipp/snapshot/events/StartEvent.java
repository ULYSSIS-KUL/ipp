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
package org.ulyssis.ipp.snapshot.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.control.commands.SetStartTimeCommand;
import org.ulyssis.ipp.snapshot.Snapshot;

import java.time.Instant;

@JsonTypeName("Start")
public final class StartEvent extends Event {
    /**
     * Private constructor for Jackson
     */
    private StartEvent() {
        super(Instant.MIN);
    }

    public StartEvent(Instant time) {
        super(time);
    }

    public Snapshot apply(Snapshot snapshot) {
        return Snapshot.builder(getTime()).fromSnapshot(snapshot).withStartTime(getTime()).build();
    }

    public static StartEvent fromCommand(Command command) {
        assert(command instanceof SetStartTimeCommand);
        SetStartTimeCommand cmd = (SetStartTimeCommand) command;
        return new StartEvent(cmd.getTime());
    }

    @Override
    public boolean unique() {
        return true;
    }
}
