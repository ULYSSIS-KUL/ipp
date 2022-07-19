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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.runtime.Runner;

import java.io.IOException;

public final class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    private Main() {}

    public static void main(String[] args) {
        Runner runner = new Runner();
        try {
            PublisherOptions.publisherOptionsFromArgs(args).ifPresent(options -> {
                Config.fromConfigurationFile(options.getConfigFile()).ifPresent(config -> {
                    Config.setCurrentConfig(config);
                    if (options.getSource() == PublisherOptions.Source.HTTP) {
                        LOG.info("Creating HttpServerPublisher");
                        try {
                            runner.addRunnable(new HttpServerPublisher(options));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        LOG.info("Creating DatabasePublisher");
                        try {
                            runner.addRunnable(new DatabasePublisher(options));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    runner.run();
                });
            });
        } finally {
            runner.cleanup();
        }
    }
}
