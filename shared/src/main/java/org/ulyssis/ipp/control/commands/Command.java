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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({ @JsonSubTypes.Type(value=AddTagCommand.class),
        @JsonSubTypes.Type(value=CorrectionCommand.class),
        @JsonSubTypes.Type(value=PingCommand.class),
        @JsonSubTypes.Type(value=RemoveTagCommand.class),
        @JsonSubTypes.Type(value=SetEndTimeCommand.class),
        @JsonSubTypes.Type(value=SetStartTimeCommand.class),
        @JsonSubTypes.Type(value=SetStatusMessageCommand.class),
        @JsonSubTypes.Type(value=SetStatusCommand.class),
        @JsonSubTypes.Type(value=SetUpdateFrequencyCommand.class) })
public abstract class Command {
    private final String commandId;
    private final Instant time;

    public Command() {
        this.commandId = generateCommandId();
        this.time = Instant.now();
    }

    public Command(Instant time) {
        this.commandId = generateCommandId();
        this.time = time;
    }

    public Command(String commandId, Instant time) {
        this.commandId = commandId;
        this.time = time;
    }

    public static String generateCommandId() {
        return UUID.randomUUID().toString();
    }

    public String getCommandId() {
        return commandId;
    }

    public Instant getTime() {
        return time;
    }
}
