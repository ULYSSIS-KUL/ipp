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
package org.ulyssis.ipp.reader;

import org.kohsuke.args4j.Option;
import org.ulyssis.ipp.config.Options;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class ReaderOptions extends Options {
    @Option(name="-id", usage="Set the id of this reader, " +
            "this is the index in the array in the configuration.", aliases="--id",
            metaVar="<id>", required=true)
    private int id;

    @Option(name="--no-redis", usage="Don't connect to Redis. This can be used if you just want to "
            + "run it standalone and log stuff.")
    private boolean noRedis = false;

    @Option(name="--replay-file-prefix", usage="The prefix to log updates to, so that it can be replayed."
            + "Set to \"-\" to disable. Defaults to \"replaylog-\".", metaVar="<log-prefix>")
    private String replayFilePrefix = "replaylog-";

    @Option(name="--replay-file", usage="The file to log updates to or read updates from, for replays.",
            metaVar="<replay-file>")
    private String replayFile = "-";

    private ReaderOptions() {
    }

    private Optional<Optional<Path>> actualReplayFile = Optional.empty();

    public int getId() {
        return id;
    }

    public boolean getNoRedis() {
        return noRedis;
    }

    @Override
    protected String getExecName() {
        return "Reader";
    }

    public static Optional<ReaderOptions> readerOptionsFromArgs(String[] args) {
        Optional<Options> options = (new ReaderOptions().doFromArgs(args));
        if (options.isPresent()) {
            return Optional.of((ReaderOptions) options.get());
        } else {
            return Optional.empty();
        }
    }

    public Optional<Path> getReplayFile() {
        if (!actualReplayFile.isPresent()) {
            synchronized (actualReplayFile) {
                if (!actualReplayFile.isPresent()) {
                    if (!replayFile.equals("-")) {
                        actualReplayFile = Optional.of(Optional.of(Paths.get(replayFile)));
                    } else if (getReplayFileEnabled()) {
                        actualReplayFile = Optional.of(Optional.of(Paths.get(replayFilePrefix +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".json")));
                    } else {
                        actualReplayFile = Optional.of(Optional.empty());
                    }
                }
            }
        }
        return actualReplayFile.get();
    }

    public boolean getReplayFileEnabled() {
        return !replayFilePrefix.equals("-");
    }
}
