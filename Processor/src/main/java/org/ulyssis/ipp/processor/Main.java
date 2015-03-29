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
package org.ulyssis.ipp.processor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.ulyssis.ipp.config.Config;

import java.util.ArrayList;
import java.util.Collection;

public final class Main {
    private final Collection<Thread> threads = new ArrayList<>();
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
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopThreads));
        doRun();
    }

    private void doRun() {
        ProcessorOptions.processorOptionsFromArgs(args).ifPresent(options ->
            Config.fromConfigurationFile(options.getConfigFile()).ifPresent(config -> {
                Config.setCurrentConfig(config);
                spawn(new Processor(options));
                try {
                    for (Thread thread : threads) {
                        thread.join();
                    }
                    Configurator.shutdown((LoggerContext)LogManager.getContext());
                } catch (InterruptedException e) {
                    // Ignore
                }
            })
        );
    }

    private void stopThreads() {
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }

    public static void main(String[] args) {
        Main main = new Main(args);
        main.run();
    }
}
