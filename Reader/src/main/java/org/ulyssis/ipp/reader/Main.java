package org.ulyssis.ipp.reader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.LoggerContext;
import org.ulyssis.ipp.config.Config;

import java.util.ArrayList;
import java.util.Collection;

public final class Main {
    private final Collection<Thread> threads = new ArrayList<>();
    private boolean restart = false;
    private final String[] args;

    private Main(String[] args) {
        this.args = args;
    }

    private void spawn(Runnable runnable) {
        Thread thread = new Thread(runnable);
        threads.add(thread);
        thread.start();
    }

    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::interruptHook));
        do {
            restart = false;
            doRun();
            if (restart) {
                stopAndJoinThreads();
            }
        } while (restart);
    }

    private void doRun() {
        ReaderOptions.readerOptionsFromArgs(args).ifPresent(options ->
            Config.fromConfigurationFile(options.getConfigFile()).ifPresent(config -> {
                Config.setCurrentConfig(config);
                spawn(new Reader(options, this::restartHook));
                try {
                    while(!Thread.currentThread().isInterrupted() && !restart) {
                        Thread.sleep(1000L);
                    }
                } catch (InterruptedException e) {
                    // Ignore
                }
            })
        );
    }

    private void interruptHook() {
        stopAndJoinThreads();
        Configurator.shutdown((LoggerContext) LogManager.getContext());
    }

    private void stopAndJoinThreads() {
        for (Thread thread : threads) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void restartHook() {
        restart = true;
    }

    public static void main(String[] args) {
        Main main = new Main(args);
        main.run();
    }
}