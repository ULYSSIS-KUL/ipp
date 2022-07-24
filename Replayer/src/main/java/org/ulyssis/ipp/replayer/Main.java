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

import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.runtime.Runner;

// TODO: Replay files
public final class Main {
    public static void main(String[] args) {
        Runner runner = new Runner();
        try {
            ReplayerOptions.replayerOptionsFromArgs(args).ifPresent(options ->
                Config.fromConfigurationFile(options.getConfigFile()).ifPresent(config -> {
                    if (options.getSpeedFactor() != 1D) {
                        System.out.println("Speed factor is not equal to 1 - remember to " +
                                "adjust `maxSpeedKmPerH` in the Processor config accordingly!");
                        // TODO: Maybe there is a cleaner way than requiring the user to do the above.
                    }
                    Config.setCurrentConfig(config);
                    runner.addRunnable(new Replayer(options));
                    runner.run();
                })
            );
        } finally {
            runner.cleanup();
        }
    }
}
