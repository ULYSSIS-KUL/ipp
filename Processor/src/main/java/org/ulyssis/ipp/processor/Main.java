package org.ulyssis.ipp.processor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.ulyssis.ipp.config.Config;

import java.util.ArrayList;
import java.util.Collection;

public final class Main {
    private final Collection<Thread> threads = new ArrayList<>();
    private boolean restart = false;
    private boolean stop = false;
    private final String[] args;

    private Main(String[] args) {
        this.args = args;
    }

    private void spawn(Runnable runnable) {
        Thread thread = new Thread(runnable);
        threads.add(thread);
        thread.start();
    }

    private void run() {
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
        ProcessorOptions.processorOptionsFromArgs(args).ifPresent(options ->
            Config.fromConfigurationFile(options.getConfigFile()).ifPresent(config -> {
                Config.setCurrentConfig(config);
                spawn(new Processor(options, this::restartHook));
                try {
                    while(!stop && !restart) {
                        Thread.sleep(1000L);
                    }
                } catch (InterruptedException e) {
                    // Ignore
                }
            })
        );
    }

    private void interruptHook() {
        stop = true;
        stopAndJoinThreads();
        Configurator.shutdown((LoggerContext)LogManager.getContext());
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
