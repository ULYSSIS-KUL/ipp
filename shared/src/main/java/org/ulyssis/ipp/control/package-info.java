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
/**
 * = The command subsystem
 *
 * All commands are (currently) relayed as JSON using
 * Redis pubsub channels. They can be dispatched with
 * CommandDispatcher, and processed with a CommandProcessor
 * where a CommandHandler can be registered for each
 * Command class.
 */
package org.ulyssis.ipp.control;
