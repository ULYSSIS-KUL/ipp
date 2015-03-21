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
package org.ulyssis.ipp.control.handlers;

import org.ulyssis.ipp.control.CommandHandler;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.snapshot.events.Event;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class EventCommandHandler<EventT extends Event> implements CommandHandler {
    private final Class<? extends Command> clazz;
    private final Function<Command, EventT> toEvent;
    private final BiConsumer<Event, Consumer<Boolean>> processCallback;

    public EventCommandHandler(Class<? extends Command> clazz,
                               Function<Command, EventT> toEvent,
                               BiConsumer<Event, Consumer<Boolean>> processCallback) {
        this.clazz = clazz;
        this.toEvent = toEvent;
        this.processCallback = processCallback;
    }

    @Override
    public void handle(Command command, Consumer<Boolean> callback) {
        Event event = toEvent.apply(command);
        processCallback.accept(event, callback);
    }

    @Override
    public Class<? extends Command> getCommandClass() {
        return clazz;
    }
}
