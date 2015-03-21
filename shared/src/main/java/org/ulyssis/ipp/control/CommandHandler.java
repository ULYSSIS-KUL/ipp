package org.ulyssis.ipp.control;

import org.ulyssis.ipp.control.commands.Command;

import java.util.function.Consumer;

/**
 * = The CommandHandler interface.
 *
 * Implement it for a certain Command, and register it with a
 * CommandProcessor in order to handle commands.
 */
public interface CommandHandler {
    /**
     * = Handle the given command, return false if unable to handle.
     *
     * @param command
     *        The command to handle
     * @param callback
     *        The callback for success/failure
     */
    public void handle(Command command, Consumer<Boolean> callback);

    /**
     * = Returns the type of command supported by this handler.
     */
    public Class<? extends Command> getCommandClass();
}
