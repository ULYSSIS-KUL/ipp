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
import org.kohsuke.args4j.Option;
import org.ulyssis.ipp.config.Options;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

public final class PublisherOptions extends Options {
    private static final Logger LOG = LogManager.getLogger(PublisherOptions.class);

    @Option(name="-r", usage="The URI of the Redis server to use, defaults to redis://localhost", aliases="--redis", metaVar="<redis URI>")
    private URI redisUri;
    {
        try {
            redisUri = new URI("redis://localhost");
        } catch (URISyntaxException e) {
            LOG.fatal("Couldn't parse the default URI?!", e);
        }
    }

    @Option(name="--database", usage="The database to retrieve the latest snapshot from", metaVar="<uri>", required=true)
    private URI databaseUri;

    @Option(name="-o", usage="The file to write the score to", aliases={"--out", "--output"}, metaVar="<output>", required=true)
    private Path outputFile;

    @Option(name="--http", usage="The HTTP host to push the score to", metaVar="<uri>", required=false)
    private URL http = null;

    public static Optional<PublisherOptions> publisherOptionsFromArgs(String[] args) {
        Optional<Options> options = (new PublisherOptions().doFromArgs(args));
        if (options.isPresent()) {
            return Optional.of((PublisherOptions) options.get());
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

    public Path getOutputFile() {
        return outputFile;
    }

    public URL getHttp() {
        return http;
    }
}
