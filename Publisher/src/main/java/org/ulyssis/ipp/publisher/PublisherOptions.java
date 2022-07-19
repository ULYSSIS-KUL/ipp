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

    public enum Source {
        HTTP,
        DATABASE
    }

    @Option(name="-r", usage="The URI of the Redis server to use, defaults to redis://localhost", aliases="--redis", metaVar="<redis URI>")
    private URI redisUri;
    {
        try {
            redisUri = new URI("redis://localhost");
        } catch (URISyntaxException e) {
            LOG.fatal("Couldn't parse the default URI?!", e);
        }
    }

    @Option(name="--database", usage="The database to retrieve the latest snapshot from", metaVar="<uri>", required=false)
    private URI databaseUri = null;

    @Option(name="-p", usage="The port to listen on", aliases={"--port"}, metaVar="<port>", required=false)
    private Integer port = null;

    @Option(name="--host", usage="The host to listen on, if a port is also specified", metaVar="<host>", required=false)
    private String host = "0.0.0.0";

    @Option(name="-o", usage="The file to write the score to", aliases={"--out", "--output"}, metaVar="<output>", required=false)
    private Path outputFile = null;

    @Option(name="--http", usage="The HTTP host to push the score to", metaVar="<uri>", required=false)
    private URL http = null;

    @Option(name="--hmac-key-file", usage="The path to the file containing the HMAC key", metaVar="<hmacKeyFile>", required=false)
    private Path hmacKeyFile = null;

    @Option(name="--tmpdir", usage="The temporary directory to use", metaVar="<tmpdir>", required=false)
    private Path tmpdir = null;

    private static final ExtraCondition condition = new ExtraCondition() {
        @Override
        public boolean evaluate(Options options) {
            PublisherOptions pOptions = (PublisherOptions)options;
            if (pOptions.getOutputFile() == null && pOptions.getHttp() == null) {
                message = "You must specify either an output file or a http URL (or both)";
                return false;
            }
            if (pOptions.getDatabaseUri() == null && pOptions.getPort() == null) {
                message = "You must specify either a database URI or a port to listen on";
                return false;
            }
            if (pOptions.getDatabaseUri() != null && pOptions.getPort() != null) {
                message = "You can not both connect to a database, and listen on a port, choose one!";
                return false;
            }
            if (pOptions.getPort() != null && pOptions.getHmacKeyFilePath() == null) {
                message = "You must provide a HMAC key when listening on a port.";
                return false;
            }
            if (pOptions.getHttp() != null && pOptions.getHmacKeyFilePath() == null) {
                message = "You must provide a HMAC key when publishing to an HTTP endpoint.";
                return false;
            }
            return true;
        }
    };

    public static Optional<PublisherOptions> publisherOptionsFromArgs(String[] args) {
        Optional<Options> options = (new PublisherOptions().doFromArgs(args, condition));
        if (options.isPresent()) {
            return Optional.of((PublisherOptions) options.get());
        } else {
            return Optional.empty();
        }
    }

    public Source getSource() {
        if (port != null) {
            return Source.HTTP;
        } else {
            return Source.DATABASE;
        }
    }

    public URI getRedisUri() {
        return redisUri;
    }

    public URI getDatabaseUri() {
        return databaseUri;
    }

    public Integer getPort() {
        return port;
    }

    public String getHost() { return host; }

    public Path getOutputFile() {
        return outputFile;
    }

    public URL getHttp() {
        return http;
    }

    public Path getHmacKeyFilePath() { return hmacKeyFile; }

    public Optional<Path> getTmpDir() {
        return Optional.ofNullable(tmpdir);
    }
}
