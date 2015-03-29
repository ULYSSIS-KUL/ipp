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
package org.ulyssis.ipp.replayer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.ulyssis.ipp.config.Config;

import java.util.ArrayList;
import java.util.Collection;

// TODO: Replay files
// TODO: With an offset (X minutes until we send the first sample)
// TODO: With time scaling, NOTE: this changes rejection of events!
public final class Main {
    private final Collection<Thread> threads = new ArrayList<>();
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
        doRun();
    }

    private void doRun() {
        ReplayerOptions.replayerOptionsFromArgs(args).ifPresent(options ->
            Config.fromConfigurationFile(options.getConfigFile()).ifPresent(config -> {
                Config.setCurrentConfig(config);
                spawn(new Replayer(options));
            })
        );
        cleanup();
    }

    private void interruptHook() {
        for (Thread thread : threads) {
            thread.interrupt();
        }
        cleanup();
    }

    private void cleanup() {
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
        }
        Configurator.shutdown((LoggerContext) LogManager.getContext());
    }

    public static void main(String[] args) {
        Main main = new Main(args);
        main.run();
    }
}
