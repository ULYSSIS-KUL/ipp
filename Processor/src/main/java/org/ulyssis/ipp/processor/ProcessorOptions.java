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

import org.kohsuke.args4j.Option;
import org.ulyssis.ipp.config.Options;

import java.net.URI;
import java.util.Optional;

public final class ProcessorOptions extends Options {
    @Option(name="--redis", usage="The URI of the redis instance for this processor, defaults to redis://127.0.0.1",
            metaVar="<uri>", required=false)
    private URI redisUri = URI.create("redis://127.0.0.1");
    @Option(name="--clone", usage="Set this variable to clone the state of another processor",
            metaVar="<uri>", required=false)
    private URI clone = null;

    private ProcessorOptions() {
    }

    @Override
    protected String getExecName() {
        return "Processor";
    }

    public static Optional<ProcessorOptions> processorOptionsFromArgs(String[] args) {
        Optional<Options> options = (new ProcessorOptions().doFromArgs(args));
        if (options.isPresent()) {
            return Optional.of((ProcessorOptions) options.get());
        } else {
            return Optional.empty();
        }
    }

    public URI getRedisUri() {
        return redisUri;
    }

    public URI getClone() {
        return clone;
    }
}
