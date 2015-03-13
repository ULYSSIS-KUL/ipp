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
