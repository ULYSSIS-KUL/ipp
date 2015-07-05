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
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.control.commands.SetUpdateFrequencyCommand;

import java.time.Instant;

@JsonTypeName("UpdateFrequencyChange")
public final class UpdateFrequencyChangeEvent extends Event {
    private final int updateFrequency;

    @JsonCreator
    public UpdateFrequencyChangeEvent(Instant time, int updateFrequency) {
        super(time);
        this.updateFrequency = updateFrequency;
    }

    @Override
    protected Snapshot doApply(Snapshot before) {
        return Snapshot.builder(getTime()).fromSnapshot(before).withUpdateFrequency(updateFrequency).build();
    }

    public int getUpdateFrequency() {
        return updateFrequency;
    }

    public static UpdateFrequencyChangeEvent fromCommand(Command command) {
        assert(command instanceof SetUpdateFrequencyCommand);
        SetUpdateFrequencyCommand cmd = (SetUpdateFrequencyCommand) command;
        return new UpdateFrequencyChangeEvent(cmd.getTime(), cmd.getUpdateFrequency());
    }
}
