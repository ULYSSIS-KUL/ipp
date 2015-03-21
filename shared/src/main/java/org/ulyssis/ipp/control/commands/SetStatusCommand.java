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
package org.ulyssis.ipp.control.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.ulyssis.ipp.updates.Status;

import java.time.Instant;

@JsonTypeName("SetStatus")
public final class SetStatusCommand extends Command {
    private final Status status;

    public SetStatusCommand(Status status) {
        super();
        this.status = status;
    }

    @JsonCreator
    private SetStatusCommand(@JsonProperty("time") Instant time,
                             @JsonProperty("status") Status status) {
        super(time);
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
