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
import org.ulyssis.ipp.control.commands.SetStatusCommand;
import org.ulyssis.ipp.updates.Status;

import java.time.Instant;

@JsonTypeName("StatusChange")
public final class StatusChangeEvent extends Event {
    private final Status status;

    @JsonCreator
    public StatusChangeEvent(@JsonProperty("time") Instant time,
                             @JsonProperty("status")Status status) {
        super(time);
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public static StatusChangeEvent fromCommand(Command command) {
        assert(command instanceof SetStatusCommand);
        SetStatusCommand cmd = (SetStatusCommand) command;
        return new StatusChangeEvent(cmd.getTime(), cmd.getStatus());
    }

    @Override
    protected Snapshot doApply(Snapshot before) {
        Snapshot.Builder builder = Snapshot.builder(getTime(), before).withStatus(status);
        if (!before.getStatus().isPublic() && status.isPublic()) {
            builder.withPublicTeamStates(before.getTeamStates());
        }
        return builder.build();
    }

    @Override
    public boolean isRemovable() {
        return true;
    }
}
