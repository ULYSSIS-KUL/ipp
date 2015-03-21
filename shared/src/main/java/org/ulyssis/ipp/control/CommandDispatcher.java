package org.ulyssis.ipp.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.status.StatusMessage;
import org.ulyssis.ipp.utils.JedisHelper;
import org.ulyssis.ipp.utils.Serialization;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;

public final class CommandDispatcher implements Runnable {
    private static final Logger LOG = LogManager.getLogger(CommandDispatcher.class);

    /**
     * = The result of running a command
     */
    public enum Result {
        /**
         * The command was successfully executed
         */
        SUCCESS,
        /**
         * The command is unsupported by the target
         */
        UNSUPPORTED,
        /**
         * The command failed for some reason
         */
        ERROR,
        /**
         * The command execution timed out.
         * <p>
         * Note that at any time, a timeout may still be followed by a success result,
         * if it was received correctly.
         */
        TIMEOUT
    }

    private static class ProcessingCommand {
        Command command;
        BiConsumer<Command, Result> callback;
        TimerTask timerTask;

        ProcessingCommand(Command command, BiConsumer<Command, Result> callback, TimerTask timerTask) {
            this.command = command;
            this.callback = callback;
            this.timerTask = timerTask;
        }
    }

    private final LinkedBlockingQueue<Command> commandsToSend = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<String, ProcessingCommand> processingCommands = new ConcurrentHashMap<>();
    private final Timer timeoutTimer = new Timer();

    private final URI redisUri;
    private final Jedis jedis;
    private final byte[] controlChannel;
    private final byte[] statusChannel;

    public CommandDispatcher(URI redisUri, String controlChannel, String statusChannel) {
        this.redisUri = redisUri;
        this.jedis = JedisHelper.get(redisUri);
        this.controlChannel = JedisHelper.dbLocalChannel(controlChannel, redisUri).getBytes();
        this.statusChannel = JedisHelper.dbLocalChannel(statusChannel, redisUri).getBytes();
    }

    public void run() {
        Thread statusThread =
                new Thread(() -> {
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            try {
                                JedisHelper.get(redisUri).subscribe(createResultListener(), statusChannel);
                            } catch (JedisConnectionException e) {
                                // TODO: After a while, deregister the processor?
                                LOG.error("Connection with Redis was broken! Trying again in 0.5s.", e);
                                Thread.sleep(500L);
                            }
                        }
                    } catch (InterruptedException ignored) {
                    }
                });
        statusThread.start();
        while (!Thread.interrupted()) {
            try {
                Command command = commandsToSend.take();
                LOG.debug("Sending command {}", command.getCommandId());
                jedis.publish(controlChannel, Serialization.getJsonMapper().writeValueAsBytes(command));
            } catch (InterruptedException ignored) {
            } catch (JsonProcessingException e) {
                LOG.error("Error writing command as JSON object", e);
            }
        }
        statusThread.interrupt();
        try {
            statusThread.join();
        } catch (InterruptedException ignored) {
        }
    }

    private BinaryJedisPubSub createResultListener() {
        JedisHelper.BinaryCallBackPubSub pubSub = new JedisHelper.BinaryCallBackPubSub();
        pubSub.addOnMessageListener(this::onMessage);
        return pubSub;
    }

    private void onMessage(byte[] channel, byte[] message) {
        assert(Arrays.equals(channel, statusChannel));
        try {
            StatusMessage statusMessage = Serialization.getJsonMapper().readValue(message, StatusMessage.class);
            StatusMessage.MessageType type = statusMessage.getType();
            String commandId = statusMessage.getDetails();
            switch (type) {
                case COMMAND_COMPLETE:
                    handleResult(commandId, Result.SUCCESS);
                    break;
                case COMMAND_FAILED:
                    handleResult(commandId, Result.ERROR);
                    break;
                case COMMAND_UNSUPPORTED:
                    handleResult(commandId, Result.UNSUPPORTED);
                    break;
                default:
                    // LOG.debug("Command dispatcher got unsupported message type: {}", type.toString());
            }
        } catch (IOException e) {
            LOG.error("Couldn't read status message: {}", new String(message), e);
        }
    }

    public Result send(Command command) {
        final CompletableFuture<Result> future = new CompletableFuture<>();
        sendAsync(command, (c, r) -> {
            assert (c == command);
            future.complete(r);
        });
        try {
            return future.get();
        } catch (InterruptedException e) {
            return Result.TIMEOUT;
        } catch (ExecutionException e) {
            LOG.error("We got an ExecutionException. This should not happen.", e.getCause());
            return Result.ERROR;
        }
    }
    
    public void sendAsync(Command command) {
    	sendAsync(command, (c,r) -> {});
    }

    public void sendAsync(Command command, BiConsumer<Command, Result> callback) {
        TimerTask timerTask = new TimerTask() {
            public void run() {
                handleResult(command.getCommandId(), Result.TIMEOUT);
            }
        };
        processingCommands.put(command.getCommandId(), new ProcessingCommand(command, callback, timerTask));
        timeoutTimer.schedule(timerTask, 10000L);
        commandsToSend.add(command);
    }

    private synchronized void handleResult(String commandId, Result result) {
    	LOG.debug("Handled command {}", commandId);
    	if (commandId == null) {
    		return;
    	}
        ProcessingCommand processingCommand = processingCommands.get(commandId);
        if (processingCommand == null) {
            return;
        }
        processingCommands.remove(commandId);
        processingCommand.timerTask.cancel();
        processingCommand.callback.accept(processingCommand.command, result);
    }
}
