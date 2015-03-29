package org.ulyssis.ipp.replayer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.config.Config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.SortedMap;

public final class Replayer implements Runnable {
    private static final Logger LOG = LogManager.getLogger(Replayer.class);
    private final ReplayerOptions options;

    public Replayer(ReplayerOptions options) {
        this.options = options;
    }

    @Override
    public void run() {
        System.out.println("Will run the replayer with the following replay files:");
        Config config = Config.getCurrentConfig();
        int nbReaders = config.getNbReaders();
        SortedMap<Integer,Path> replayMap = options.getReplayMap();
        for (int i = 0; i < nbReaders; i++) {
            if (replayMap.containsKey(i)) {
                System.out.println(String.format("Reader %d: %s", i, replayMap.get(i).toAbsolutePath()));
            }
        }
        System.out.println("Press any key to continue (or ctrl-C to cancel)");
        try {
            System.in.read();
            while (!Thread.currentThread().isInterrupted()) {}
        } catch (IOException e) {
            LOG.fatal("An unexpected exception occurred!", e);
        }
    }
}
