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
