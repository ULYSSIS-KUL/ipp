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
package org.ulyssis.ipp.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Defines all command line options that can be set for an instance of the system.
 * 
 * Care should be taken to ensure that this class is immutable. It is
 * only written to when creating an Options object from a command line.
 */
public class Options {
    @Option(name="-c", usage="The configuration file", aliases="--config", metaVar="<config>")
    private String configFile = "config.json";

    protected Options() {
    }

    /**
     * Create options from the given arguments.
     *
     * @param args
     *           The argument array representing the command line.
     * @return empty if the options are invalid, or no options are generated
     *            (e.g. when using the -h option), or a valid Options object.
     */
    public static Optional<Options> fromArgs(String[] args) {
        return (new Options().doFromArgs(args));
    }

    protected String getExecName() {
        return "Executable";
    }

    protected Optional<Options> doFromArgs(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            return Optional.of(this);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.printf("Usage: %s [options]\n", getExecName());
            parser.printUsage(System.err);
            return Optional.empty();
        }
    }

    public Path getConfigFile() {
        return Paths.get(configFile);
    }
}
