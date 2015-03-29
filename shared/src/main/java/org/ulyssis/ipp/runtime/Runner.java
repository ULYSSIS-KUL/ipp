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
package org.ulyssis.ipp.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import java.util.ArrayList;
import java.util.Collection;

public final class Runner {
    private final Collection<Thread> threads = new ArrayList<>();
    private final Collection<Runnable> runnables = new ArrayList<>();

    public Runner() {
    }

    public void addRunnable(Runnable runnable) {
        this.runnables.add(runnable);
    }

    private void spawn(Runnable runnable) {
        Thread thread = new Thread(runnable);
        threads.add(thread);
        thread.start();
    }

    // TODO: What about LogManager cleanup when parsing options and stuff?
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::interruptHook));
        for (Runnable runnable : runnables) {
            spawn(runnable);
        }
    }

    private void interruptHook() {
        for (Thread thread : threads) {
            thread.interrupt();
        }
        cleanup();
    }

    public void cleanup() {
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
        }
        Configurator.shutdown((LoggerContext) LogManager.getContext());
    }
}
