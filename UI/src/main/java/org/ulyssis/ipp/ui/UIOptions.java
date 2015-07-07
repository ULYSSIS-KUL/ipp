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
package org.ulyssis.ipp.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.Option;
import org.ulyssis.ipp.config.Options;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public final class UIOptions extends Options {
    private static final Logger LOG = LogManager.getLogger(UIOptions.class);

    @Option(name="-p", usage="The port that the UI should be served at",
            aliases="--port", metaVar="<port>", required=false)
    private int port = 8080;
    @Option(name="-H", usage="The host or IP address that the UI should be served at, defaults to all interfaces",
            aliases="--host", metaVar="<host>", required=false)
    private String host = "0.0.0.0";
    @Option(name="-r", usage="The URI of the Redis server to use, defaults to redis://localhost", aliases="--redis", metaVar="<redis URI>")
    private URI redisUri;
    {
        try {
            redisUri = new URI("redis://localhost");
        } catch (URISyntaxException e) {
            LOG.fatal("Couldn't parse the default URI?!", e);
        }
    }
    @Option(name="--database", usage="The database to connect to, in JDBC connection string format",
            metaVar="<uri>", required=true)
    private URI databaseUri = null;

    private UIOptions() {
    }

    @Override
    protected String getExecName() {
        return "UI";
    }

    public static Optional<UIOptions> uiOptionsFromArgs(String[] args) {
        Optional<Options> options = (new UIOptions().doFromArgs(args));
        if (options.isPresent()) {
            return Optional.of((UIOptions) options.get());
        } else {
            return Optional.empty();
        }
    }

    public URI getRedisUri() {
        return redisUri;
    }

    public URI getDatabaseUri() {
        return databaseUri;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }
}
