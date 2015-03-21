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
