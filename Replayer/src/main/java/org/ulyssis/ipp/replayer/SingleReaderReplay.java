package org.ulyssis.ipp.replayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.updates.TagUpdate;
import org.ulyssis.ipp.utils.Serialization;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

// TODO: SingleReaderReplay is a shitty name
public final class SingleReaderReplay {
    private static final Logger LOG = LogManager.getLogger(SingleReaderReplay.class);

    private final int id;
    private ObjectMapper jsonMapper;
    private BufferedReader reader;
    private TagUpdate nextUpdate;
    private Exception exception = null;
    private long sn = 0;

    public SingleReaderReplay(int readerId, Path replayFile) throws Exception {
        id = readerId;
        try {
            reader = Files.newBufferedReader(replayFile);
            jsonMapper = Serialization.getJsonMapper();
            fetchNextUpdate();
        } catch (Exception e) {
            reader.close();
            throw e;
        }
    }

    private void fetchNextUpdate() throws Exception {
        try {
            do {
                String line = reader.readLine();
                if (line != null) {
                    nextUpdate = jsonMapper.readValue(line, TagUpdate.class);
                    nextUpdate = new TagUpdate(nextUpdate.getReaderId(),
                            sn, nextUpdate.getUpdateTime(), nextUpdate.getTag());
                    sn ++;
                } else {
                    nextUpdate = null;
                }
            } while (nextUpdate != null && nextUpdate.getReaderId() != id);
            LOG.debug("Read line: {} {} {}", nextUpdate.getTag(), nextUpdate.getReaderId(), nextUpdate.getUpdateCount());
        } catch (Exception e) {
            nextUpdate = null;
            throw e;
        }
    }

    public boolean hasNext() {
        return nextUpdate != null;
    }

    public Exception getException() {
        return exception;
    }

    public Optional<Instant> nextTime() {
        if (nextUpdate != null) {
            return Optional.of(nextUpdate.getUpdateTime());
        } else {
            return Optional.empty();
        }
    }

    public Optional<TagUpdate> next() {
        TagUpdate result = nextUpdate;
        try {
            fetchNextUpdate();
        } catch (Exception e) {
            nextUpdate = null;
            exception = e;
        }
        return Optional.ofNullable(result);
    }
}
