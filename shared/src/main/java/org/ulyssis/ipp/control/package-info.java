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