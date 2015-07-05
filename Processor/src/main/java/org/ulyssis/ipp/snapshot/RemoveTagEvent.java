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
import org.ulyssis.ipp.control.commands.RemoveTagCommand;
import org.ulyssis.ipp.TagId;

import java.time.Instant;

@JsonTypeName("RemoveTag")
public final class RemoveTagEvent extends TagEvent {
    @JsonCreator
    public RemoveTagEvent(
            @JsonProperty("time") Instant time,
            @JsonProperty("tag") TagId tag,
            @JsonProperty("teamNb") int teamNb) {
        super(time, tag, teamNb);
    }

    @Override
    protected Snapshot doApply(Snapshot snapshot) {
        TeamTagMap newTeamTagMap = snapshot.getTeamTagMap().removeTag(getTag());
        return Snapshot.builder(getTime()).fromSnapshot(snapshot).withTeamTagMap(newTeamTagMap).build();
    }

    public static RemoveTagEvent fromCommand(Command command) {
        assert(command instanceof RemoveTagCommand);
        RemoveTagCommand cmd = (RemoveTagCommand) command;
        return new RemoveTagEvent(cmd.getTime(), cmd.getTag(), cmd.getTeamNb());
    }
}
