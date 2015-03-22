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
package org.ulyssis.ipp.integrationtests;

import com.google.common.io.Files;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.control.CommandDispatcher;
import org.ulyssis.ipp.control.commands.AddTagCommand;
import org.ulyssis.ipp.control.commands.PingCommand;
import org.ulyssis.ipp.control.commands.SetEndTimeCommand;
import org.ulyssis.ipp.control.commands.SetStartTimeCommand;
import org.ulyssis.ipp.processor.Processor;
import org.ulyssis.ipp.processor.ProcessorOptions;
import org.ulyssis.ipp.status.StatusMessage;
import org.ulyssis.ipp.updates.TagUpdate;
import org.ulyssis.ipp.utils.JedisHelper;
import org.ulyssis.ipp.utils.Serialization;
import org.ulyssis.ipp.TagId;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

public class TestWithRedis {
    private static Process redisProcess;
    private static Jedis jedis;

    private BinaryJedisPubSub pubSub;
    private Semaphore semaphore;
    private Thread newSnapshotThread;

    private List<Thread> runningThreads = new ArrayList<>();

    @BeforeClass
    public static void startRedis() throws Exception {
        // Place a "redispath" file with the path to Redis in the resources folder
        Path redisPathPath = Paths.get("src", "test", "resources", "redispath");
        File redisPathFile = redisPathPath.toFile();
        Path redisPath = null;
        File redisFile = null;
        boolean foundRedis = false;
        if (redisPathFile.exists() && redisPathFile.isFile() && redisPathFile.canRead()) {
            String firstLine = Files.readFirstLine(redisPathFile, StandardCharsets.UTF_8);
            firstLine = firstLine.trim();
            redisPath = Paths.get(firstLine);
            redisFile = redisPath.toFile();
            if (redisFile.exists() && redisFile.isFile() && redisFile.canExecute()) {
                foundRedis = true;
            }
        }
        String pathEnv = System.getenv("PATH");
        String os = System.getProperty("os.name");
        String[] paths;
        if (os.startsWith("Windows")) {
            paths = pathEnv.split(";");
        } else {
            paths = pathEnv.split(":");
        }
        for (int i = 0; i < paths.length && !foundRedis; ++i) {
            if (os.startsWith("Windows")) {
                redisPath = Paths.get(paths[i], "redis-server.exe");
            } else {
                redisPath = Paths.get(paths[i], "redis-server");
            }
            redisFile = redisPath.toFile();
            if (redisFile.exists() && redisFile.isFile() && redisFile.canExecute()) {
                foundRedis = true;
            }
        }
        if (!os.startsWith("Windows")) {
            if (redisFile == null || !redisFile.exists() || !redisFile.isFile() || !redisFile.canExecute()) {
                redisPath = Paths.get("/usr/bin/redis-server");
                redisFile = redisPath.toFile();
            }
            if (redisFile == null || !redisFile.exists() || !redisFile.isFile() || !redisFile.canExecute()) {
                redisPath = Paths.get("/usr/sbin/redis-server");
                redisFile = redisPath.toFile();
            }
            if (redisFile == null || !redisFile.exists() || !redisFile.isFile() || !redisFile.canExecute()) {
                redisPath = Paths.get("/usr/local/bin/redis-server");
                redisFile = redisPath.toFile();
            }
        }
        if (redisFile == null || !redisFile.exists() || !redisFile.isFile() || !redisFile.canExecute()) {
            // Can't start Redis?
            System.err.println("Failed to start Redis: no executable file");
            return;
        }
        ProcessBuilder builder = new ProcessBuilder(redisPath.toString(),
                "--port", "12345",
                "--daemonize", "no",
                "--bind", "127.0.0.1",
                "--databases", "10",
                "--appendonly", "no",
                "--loglevel", "debug");
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        redisProcess = builder.start();
        Thread.sleep(1000L);
        jedis = JedisHelper.get(URI.create("redis://127.0.0.1:12345"));
        selfTest();
    }

    public static void selfTest() throws Exception {
        jedis.set("Bla", "Bloe");
        assertThat("Bloe", equalTo(jedis.get("Bla")));
        assertThat(1, equalTo(jedis.keys("*").size()));
    }

    @AfterClass
    public static void stopRedis() throws IOException {
        jedis.close();
        try {
            if (redisProcess.isAlive()) {
                Thread.sleep(1000L);
            }
            if (redisProcess.isAlive()) {
                redisProcess.destroy();
            }
            redisProcess.waitFor();
        } catch (InterruptedException ignored) {
        }
    }

    @Before
    @After
    public void clearRedis() {
        jedis.flushAll();
    }

    @After
    public void stopRunningThreads() {
        runningThreads.stream().forEach(thread -> {
            thread.interrupt();
            try {
                thread.join(1000L);
                // TODO: Cancel test run if thread not dead?
            } catch (InterruptedException ignored) {
            }
        });
        runningThreads.clear();
    }

    @Before
    public void startNewSnapshotThread() {
        semaphore = new Semaphore(0);
        pubSub = new JedisHelper.BinaryCallBackPubSub();
        ((JedisHelper.BinaryCallBackPubSub) pubSub).addOnMessageListener((channel, message) -> {
            try {
                StatusMessage msg = Serialization.getJsonMapper().readValue(message, StatusMessage.class);
                if (msg.getType() == StatusMessage.MessageType.NEW_SNAPSHOT) {
                    semaphore.release();
                }
            } catch (IOException ignored) {
            }
        });
        newSnapshotThread = new Thread(() -> {
            try {
                Jedis jedis = JedisHelper.get(new URI("redis://127.0.0.1:12345/0"));
                jedis.subscribe(pubSub, "status:0".getBytes());
                jedis.close();
            } catch (URISyntaxException ignored) {
            }
        });
        newSnapshotThread.start();
    }

    @After
    public void endNewSnapshotThread() throws Exception {
        pubSub.unsubscribe();
        newSnapshotThread.join();
    }

    private void waitForSnapshot() throws Exception {
        semaphore.acquire();
    }

    private void setConfig(String start, String... rest) {
        Config config = Config.fromConfigurationFile(Paths.get(start, rest)).get();
        Config.setCurrentConfig(config);
    }

    private CommandDispatcher spawnProcessor(String... args) throws InterruptedException {
        ProcessorOptions options = ProcessorOptions.processorOptionsFromArgs(args).get();
        Processor processor = new Processor(options, () -> {});
        Semaphore sem = new Semaphore(0);
        processor.addOnStartedCallback(p -> sem.release());
        Thread thread = new Thread(processor);
        runningThreads.add(thread);
        thread.start();
        sem.acquire();
        Config config = Config.getCurrentConfig();
        CommandDispatcher dispatcher = new CommandDispatcher(options.getRedisUri(), config.getControlChannel(), config.getStatusChannel());
        Thread dispatcherThread = new Thread(dispatcher);
        runningThreads.add(dispatcherThread);
        dispatcherThread.start();
        return dispatcher;
    }

    @Test
    public void testPing() throws Exception {
        setConfig("src", "test", "resources", "config1.json");
        CommandDispatcher dispatcher = spawnProcessor("--redis", "redis://127.0.0.1:12345/0");
        CommandDispatcher.Result result = dispatcher.send(new PingCommand());
        assertThat(result, equalTo(CommandDispatcher.Result.SUCCESS));
    }

    @Test
    public void testAddTag() throws Exception {
        setConfig("src", "test", "resources", "config1.json");
        CommandDispatcher dispatcher = spawnProcessor("--redis", "redis://127.0.0.1:12345/0");
        CommandDispatcher.Result result = dispatcher.send(
                new AddTagCommand(new TagId("abcd"), 0));
        assertThat(result, equalTo(CommandDispatcher.Result.SUCCESS));
        assertThat(jedis.get("snapshot"), sameJSONAs("{teamTagMap:{0:[\"ABCD\"]}}").allowingExtraUnexpectedFields());
        assertThat(jedis.llen("snapshots"), equalTo(1L));
        assertThat(jedis.rpop("snapshots"), sameJSONAs("{teamTagMap:{0:[\"ABCD\"]}}").allowingExtraUnexpectedFields());
    }

    @Test
    public void testSetStartTime() throws Exception {
        setConfig("src", "test", "resources", "config1.json");
        CommandDispatcher dispatcher = spawnProcessor("--redis", "redis://127.0.0.1:12345/0");
        CommandDispatcher.Result result = dispatcher.send(
                new SetStartTimeCommand(Instant.EPOCH));
        assertThat(result, equalTo(CommandDispatcher.Result.SUCCESS));
        assertThat(jedis.get("snapshot"), sameJSONAs("{startTime:0}").allowingExtraUnexpectedFields());
        assertThat(jedis.llen("snapshots"), equalTo(1L));
        assertThat(jedis.rpop("snapshots"), sameJSONAs("{startTime:0}").allowingExtraUnexpectedFields());
    }

    @Test
    public void testSetStopTime() throws Exception {
        setConfig("src", "test", "resources", "config1.json");
        CommandDispatcher dispatcher = spawnProcessor("--redis", "redis://127.0.0.1:12345/0");
        CommandDispatcher.Result result = dispatcher.send(
                new SetEndTimeCommand(Instant.EPOCH));
        assertThat(result, equalTo(CommandDispatcher.Result.SUCCESS));
        assertThat(jedis.get("snapshot"), sameJSONAs("{endTime:0}").allowingExtraUnexpectedFields());
        assertThat(jedis.llen("snapshots"), equalTo(1L));
        assertThat(jedis.rpop("snapshots"),sameJSONAs("{endTime:0}").allowingExtraUnexpectedFields());
    }

    @Test
    public void testSingleTeamNormalLap() throws Exception {
        setConfig("src", "test", "resources", "config1.json");
        CommandDispatcher dispatcher = spawnProcessor("--redis", "redis://127.0.0.1:12345/0");
        dispatcher.send(new SetStartTimeCommand(Instant.EPOCH));
        waitForSnapshot();
        dispatcher.send(new SetEndTimeCommand(Instant.EPOCH.plus(15, ChronoUnit.MINUTES)));
        waitForSnapshot();
        dispatcher.send(new AddTagCommand(Instant.EPOCH, new TagId("abcd"), 0));
        waitForSnapshot();
        Jedis readerJedis = JedisHelper.get(new URI("redis://127.0.0.1:12345/1"));
        TagUpdate update = new TagUpdate(0, 0, Instant.EPOCH.plus(3, ChronoUnit.MINUTES), new TagId("abcd"));
        readerJedis.rpush("updates".getBytes(), Serialization.getJsonMapper().writeValueAsBytes(update));
        readerJedis.publish("update:1", "0");
        waitForSnapshot();
        readerJedis.select(2);
        update = new TagUpdate(1, 0, Instant.EPOCH.plus(6, ChronoUnit.MINUTES), new TagId("abcd"));
        readerJedis.rpush("updates".getBytes(), Serialization.getJsonMapper().writeValueAsBytes(update));
        readerJedis.publish("update:2", "0");
        waitForSnapshot();
        readerJedis.select(3);
        update = new TagUpdate(2, 0, Instant.EPOCH.plus(9, ChronoUnit.MINUTES), new TagId("abcd"));
        readerJedis.rpush("updates".getBytes(), Serialization.getJsonMapper().writeValueAsBytes(update));
        readerJedis.publish("update:3", "0");
        waitForSnapshot();
        readerJedis.select(1);
        update = new TagUpdate(0, 1, Instant.EPOCH.plus(12, ChronoUnit.MINUTES), new TagId("abcd"));
        readerJedis.rpush("updates".getBytes(), Serialization.getJsonMapper().writeValueAsBytes(update));
        readerJedis.publish("update:1", "1");
        waitForSnapshot();
        readerJedis.close();
        assertThat(semaphore.availablePermits(), equalTo(0));
        assertThat(jedis.get("snapshot"), sameJSONAs("{teamTagMap:{0:[\"ABCD\"]},teamStates:{0:{tagFragmentCount:3}}}")
            .allowingExtraUnexpectedFields());
    }

    @Test
    public void testOnlyOneEvent() throws Exception {
        setConfig("src", "test", "resources", "config1.json");
        CommandDispatcher dispatcher = spawnProcessor("--redis", "redis://127.0.0.1:12345/0");
        dispatcher.send(new SetStartTimeCommand(Instant.EPOCH));
        waitForSnapshot();
        dispatcher.send(new SetStartTimeCommand(Instant.EPOCH.plus(20, ChronoUnit.SECONDS)));
        waitForSnapshot();
        Jedis jedis = JedisHelper.get(new URI("redis://127.0.0.1:12345/0"));
        assertThat(jedis.llen("snapshots"), equalTo(2L));
        assertThat(jedis.zcard("events"), equalTo(2L));
        List<String> events = new ArrayList<>(jedis.zrange("events", 0L, -1L));
        assertThat(events.get(0), sameJSONAs("{type:Identity}")
                .allowingExtraUnexpectedFields());
        assertThat(events.get(1), sameJSONAs("{type:Start}")
                .allowingExtraUnexpectedFields());
        assertThat(jedis.llen("snapshots"), equalTo(2L));
        assertThat(jedis.lindex("snapshots", 1L), sameJSONAs("{startTime:20}")
                .allowingExtraUnexpectedFields());
    }
}
