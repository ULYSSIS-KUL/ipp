package org.ulyssis.ipp.control;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.status.StatusMessage;
import org.ulyssis.ipp.status.StatusReporter;
import org.ulyssis.ipp.utils.JedisHelper;
import org.ulyssis.ipp.utils.Serialization;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class CommandProcessor implements Runnable {
    private static final Logger LOG = LogManager.getLogger(CommandProcessor.class);

    private final Map<Class<? extends Command>, CommandHandler> commandHandlers = new HashMap<>();
    private final Jedis jedis;
    private final StatusReporter reporter;
    private final String channel;

    private BinaryJedisPubSub listener;

    public CommandProcessor(URI redisUri, String commandChannel, String statusChannel) {
        this(redisUri, JedisHelper.dbLocalChannel(commandChannel, redisUri),
                new StatusReporter(redisUri, statusChannel));
    }

    public CommandProcessor(URI redisUri, String channel, StatusReporter statusReporter) {
        this.jedis = JedisHelper.get(redisUri);
        this.channel = JedisHelper.dbLocalChannel(channel, redisUri);
        this.reporter = statusReporter;
    }

    public void run() {
        jedis.subscribe(createCommandListener(), channel.getBytes());
    }

    private BinaryJedisPubSub createCommandListener() {
        JedisHelper.BinaryCallBackPubSub pubSub = new JedisHelper.BinaryCallBackPubSub();
        listener = pubSub;
        pubSub.addOnMessageListener(this::onCommandMessage);
        return pubSub;
    }

    private void onCommandMessage(byte[] channel, byte[] message) {
        assert (Arrays.equals(channel, this.channel.getBytes()));
        try {
            Command command = Serialization.getJsonMapper().readValue(message, Command.class);
            handleCommand(command);
        } catch (IOException e) {
            LOG.error("Couldn't parse command: {}", new String(message), e);
        }
    }

    public void addHandler(CommandHandler handler) {
        commandHandlers.put(handler.getCommandClass(), handler);
    }

    private void handleCommand(Command command) {
        LOG.debug("Handing command {}: {}", command.getCommandId(), command.getClass().toString());
        if (commandHandlers.containsKey(command.getClass())) {
            commandHandlers.get(command.getClass()).handle(command, notifyCommandExecuted(command));
        } else {
            notifyCommandUnsupported(command);
        }
    }

    private Consumer<Boolean> notifyCommandExecuted(Command command) {
        return (result) -> {
            if (result) {
                notifySuccess(command);
            } else {
                notifyFailure(command);
            }
        };
    }

    private void notifySuccess(Command command) {
        reporter.broadcast(new StatusMessage(StatusMessage.MessageType.COMMAND_COMPLETE, command.getCommandId()));
    }

    private void notifyFailure(Command command) {
        reporter.broadcast(new StatusMessage(StatusMessage.MessageType.COMMAND_FAILED, command.getCommandId()));
    }

    private void notifyCommandUnsupported(Command command) {
        reporter.broadcast(new StatusMessage(StatusMessage.MessageType.COMMAND_UNSUPPORTED, command.getCommandId()));
    }

    public void stop() {
        try {
            listener.unsubscribe();
        } catch (JedisConnectionException ignored) {
        }
    }
}
