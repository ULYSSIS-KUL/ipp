package org.ulyssis.ipp.publisher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.Option;
import org.ulyssis.ipp.config.Options;

import java.net.URI;
import java.net.URISyntaxException;
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

    @Option(name="-o", usage="The file to write the score to", aliases={"--out", "--output"}, metaVar="<output>", required=true)
    private Path outputFile;

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

    public Path getOutputFile() {
        return outputFile;
    }
}
