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
package org.ulyssis.ipp.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A status message, to be broadcast by a StatusReporter
 *
 * @see org.ulyssis.ipp.status.StatusReporter
 */
public final class StatusMessage {
    private MessageType type;
    private String details;

    public enum MessageType {
        /**
         * The reader sends this message when no updates have been received for a while
         */
        NO_UPDATES,
        /**
         * Notifies that the system has started up
         */
        STARTED_UP,
        /**
         * Notifies that the startup has failed
         */
        STARTUP_FAILURE,
        /**
         * Notifies that the system is being shut down
         */
        SHUTDOWN,
        /**
         * Signals that the command given executed successfully.
         *
         * The details field will contain the command id.
         *
         * @see org.ulyssis.ipp.control
         */
        COMMAND_COMPLETE,
        /**
         * Signals that no command handler is installed for the given command
         *
         * The details field will contain the command id.
         *
         * @see org.ulyssis.ipp.control
         */
        COMMAND_UNSUPPORTED,
        /**
         * Signals that the command failed to execute
         *
         * The details field will contain the command id.
         *
         * @see org.ulyssis.ipp.control
         */
        COMMAND_FAILED,
        /**
         * Signals that the processor's snapshot has been updated
         */
        NEW_SNAPSHOT,
        /**
         * Signals a miscellaneous error. The details field will contain more information
         * about the error.
         */
        MISC_ERROR
    }

    /**
     * Create a new status message with the given type and details.
     *
     * This message is to be broadcast by a StatusReporter to the
     * status channel.
     *
     * @param type
     *        The type of the status
     * @param details
     *        The details. The form of this field depends
     *        on the type of message.
     * @see org.ulyssis.ipp.status.StatusReporter
     */
    @JsonCreator
    public StatusMessage(@JsonProperty("type") MessageType type,
                         @JsonProperty("details") String details) {
        this.type = type;
        this.details = details;
    }

    public MessageType getType() {
        return type;
    }

    public String getDetails() {
        return details;
    }
}
