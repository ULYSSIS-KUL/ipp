package org.ulyssis.ipp.control.handlers;

import org.ulyssis.ipp.control.CommandHandler;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.control.commands.PingCommand;

import java.util.function.Consumer;

/**
 * A simple command handler for handling ping commands.
 *
 * All it does is return success.
 */
public final class PingHandler implements CommandHandler {

    /**
     * Handles the given command, which should be a PingCommand.
     *
     * @param command
     *        The ping command to handle.
     * @param callback
     *        The ping callback, which is always invoked with true.
     */
    @Override
    public void handle(Command command, Consumer<Boolean> callback) {
        callback.accept(true);
    }

    /**
     * Returns PingCommand.class
     *
     * @return PingCommand.class
     */
    @Override
    public Class<? extends Command> getCommandClass() {
        return PingCommand.class;
    }
}
