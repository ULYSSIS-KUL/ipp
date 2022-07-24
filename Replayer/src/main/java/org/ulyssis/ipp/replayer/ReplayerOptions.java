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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.ulyssis.ipp.config.Options;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

public final class ReplayerOptions extends Options {
    @Argument(required=true,usage="The replay files to use",metaVar="<files>",multiValued=true)
    private List<String> replayFiles = new ArrayList<>();

    @Option(name="-r", usage="The Redis URI for this replayer", aliases="--redis",
            metaVar="<redis uri>")
    private URI uri = URI.create("redis://127.0.0.1");

    @Option(name="--speed", usage="Speed factor so the replay can run faster (>1) or slower (<1)")
    private double speedFactor = 1D;

    @Option(name="--delay", usage="Delay the first read by the given number of minutes.")
    private double delayMinutes = 0D;

    private SortedMap<Integer,Path> replayMap = new TreeMap<>();

    private ReplayerOptions() {
    }

    public static Optional<ReplayerOptions> replayerOptionsFromArgs(String[] args) {
        Optional<Options> options = (new ReplayerOptions().doFromArgs(args));
        if (options.isPresent()) {
            ReplayerOptions replayerOptions = (ReplayerOptions) options.get();
            List<Path> withoutId = new ArrayList<>();
            // TODO: Refactor into its own method?
            replayerOptions.replayFiles.stream().forEach(str -> {
                // TODO: What about : with Macs?
                String[] split = str.split(":", 2);
                Integer readerId = null;
                if (split.length == 2) {
                    try {
                        readerId = Integer.parseInt(split[0], 10);
                    } catch (NumberFormatException ignored) {
                        System.err.println("Expected a number before the colon, received: " + split[0]);
                    }
                }
                if (readerId == null) {
                    Path path = Paths.get(str);
                    if (path.toFile().isFile()) {
                        withoutId.add(path);
                    } else {
                        System.err.println("File not found: " + path);
                    }
                } else {
                    Path path = Paths.get(split[1]);
                    if (path.toFile().isFile()) {
                        replayerOptions.replayMap.put(readerId, path);
                    } else {
                        System.err.println("File not found: " + path);
                    }
                }
            });
            for (int i = 0, j = 0; i < replayerOptions.replayFiles.size() && j < withoutId.size(); i++) {
                if (!replayerOptions.replayMap.containsKey(i)) {
                    replayerOptions.replayMap.put(i, withoutId.get(j));
                    j++;
                }
            }
            return Optional.of(replayerOptions);
        } else {
            return Optional.empty();
        }
    }

    public SortedMap<Integer,Path> getReplayMap() {
        return Collections.unmodifiableSortedMap(replayMap);
    }

    public URI getRedisURI() {
        return uri;
    }

    public double getSpeedFactor() {
        return speedFactor;
    }

    public double getDelayMinutes() {
        return delayMinutes;
    }
}
