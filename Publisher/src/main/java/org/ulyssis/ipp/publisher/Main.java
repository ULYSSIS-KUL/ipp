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
package org.ulyssis.ipp.publisher;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.snapshot.SnapshotListener;
import org.ulyssis.ipp.utils.Serialization;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> nextProcessingOfSnapshot;

    private final SnapshotListener snapshotListener;
    private final Thread snapshotThread;
    private final PublisherOptions options;

    private final LinkedBlockingQueue<Snapshot> snapshotQueue = new LinkedBlockingQueue<>();

    private Main(PublisherOptions options) {
        this.options = options;
        Runtime.getRuntime().addShutdownHook(new Thread(this::interruptHook));
        Config.setCurrentConfig(Config.fromConfigurationFile(options.getConfigFile()).get());
        snapshotListener = new SnapshotListener(options.getRedisUri());
        snapshotThread = new Thread(snapshotListener);
        snapshotListener.addListener(onSnapshot);
        snapshotListener.trigger();
        snapshotThread.start();
        try {
            while (true) {
                Snapshot snapshot = snapshotQueue.take();
                service.submit(() -> processSnapshot(snapshot));
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void interruptHook() {
        LOG.info("Stopping Publisher");
        service.shutdownNow();
        snapshotListener.stop();
        LOG.info("Bye bye!");
        Configurator.shutdown((LoggerContext) LogManager.getContext());
    }

    private final Consumer<Snapshot> onSnapshot = snapshot -> snapshotQueue.add(snapshot);

    private void processSnapshot(Snapshot snapshot) {
        if (nextProcessingOfSnapshot != null) {
            nextProcessingOfSnapshot.cancel(false);
            nextProcessingOfSnapshot = null;
        }
        Score score = new Score(snapshot);
        try {
            File f = File.createTempFile("/tmp", ".json"); // TODO: better tmp path?
            Serialization.getJsonMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(f, score);
            Files.move(f.toPath(), options.getOutputFile(), StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Wrote new score for snapshot: {}", snapshot.getSnapshotTime());
            nextProcessingOfSnapshot = service.schedule(() -> processSnapshot(snapshot), 1L, TimeUnit.SECONDS);
        } catch (IOException e) {
            LOG.error("Error writing score", e);
        }
    }

    public static void main(String[] args) {
        Optional<PublisherOptions> optionsOptional = PublisherOptions.publisherOptionsFromArgs(args);
        if (optionsOptional.isPresent()) {
            PublisherOptions options = optionsOptional.get();
            new Main(options);
        } else {
            System.exit(-1);
        }
    }
}
