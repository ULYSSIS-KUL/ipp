package org.ulyssis.ipp.monitor;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public final class ProcessorMonitor implements Runnable {
    private static final Logger LOG = LogManager.getLogger(ProcessorMonitor.class);

    @FunctionalInterface
    public interface ProcessorFoundListener {
        public void processorFound(String id, URI uri, boolean isNew);
    }

    private final class ProcessorEntry {
        final String id;
        Instant lastSeen;
        final Set<URI> uris = new HashSet<>();

        ProcessorEntry(String id, Instant lastSeen, URI firstURI) {
            this.id = id;
            this.lastSeen = lastSeen;
            uris.add(firstURI);
        }
    }
    private final Map<String, ProcessorEntry> processors = new HashMap<>();

    private final Set<ProcessorFoundListener> listeners = new CopyOnWriteArraySet<>();
    private byte[] announceString;
    private DatagramSocket socket;

    public ProcessorMonitor(int port, String announceString) {
        this.announceString = announceString.getBytes(StandardCharsets.US_ASCII);
        try {
            this.socket = new DatagramSocket(port);
            this.socket.setBroadcast(true);
            this.socket.setReuseAddress(true);
        } catch (SocketException e) {
            LOG.error("Couldn't connect to the listening socket?", e);
        }
    }

    private static boolean byteRangeEquals(byte[] needle, byte[] haystack,
                                           int startIndexInclusive, int endIndexExclusive) {
        if (startIndexInclusive < 0 ||
                endIndexExclusive > haystack.length ||
                endIndexExclusive - startIndexInclusive > needle.length) {
            return false;
        }
        for (int i = startIndexInclusive, j = 0; i < endIndexExclusive; i++, j++) {
            if (haystack[i] != needle[j]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            byte[] buf = new byte[512];
            Arrays.fill(buf, (byte) 0);
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                LOG.debug("Receiving!");
                socket.receive(packet);
                LOG.debug("Received!");
                byte[] rn = "\r\n".getBytes(StandardCharsets.US_ASCII);
                int firstIndex = ArrayUtils.indexOf(buf, rn[0]);
                if (firstIndex == -1 || firstIndex + 2 >= buf.length || buf[firstIndex + 1] != rn[1]) {
                    LOG.debug("First carriage return not followed by newline");
                    continue;
                }
                if (!byteRangeEquals(announceString, buf, 0, firstIndex)) {
                    LOG.debug("Announce string is wrong");
                    continue;
                }
                int splitIndex = ArrayUtils.indexOf(buf, rn[0], firstIndex + 2);
                if (splitIndex == -1 || splitIndex + 2 >= buf.length || buf[splitIndex + 1] != rn[1]) {
                    LOG.debug("Next carriage return not followed by newline");
                    continue;
                }
                byte[] idBytes = ArrayUtils.subarray(buf, firstIndex + 2, splitIndex);
                String processorId = new String(idBytes, StandardCharsets.US_ASCII);
                int nextIndex = ArrayUtils.indexOf(buf, rn[0], splitIndex + 2);
                if (nextIndex == -1 || nextIndex + 1 >= buf.length || buf[nextIndex + 1] != rn[1]) {
                    LOG.debug("Next carriage return not followed by newline");
                    continue;
                }
                byte[] uriBytes = ArrayUtils.subarray(buf, splitIndex + 2, nextIndex);
                String uriStr = new String(uriBytes, StandardCharsets.US_ASCII);
                try {
                    URI uri = new URI(uriStr);
                    LOG.info("Processor seen! ID: {}, URI: {}", processorId, uriStr);
                    registerProcessor(processorId, uri);
                } catch (URISyntaxException e) {
                    LOG.debug("Faulty URI: {}", uriStr, e);
                }
            } catch (IOException e) {
                LOG.error("Error receiving packet?", e);
            }
        }
    }

    public void addProcessorFoundListener(ProcessorFoundListener listener) {
        listeners.add(listener);
    }

    public void removeProcessorFoundListener(ProcessorFoundListener listener) {
        listeners.remove(listener);
    }

    private void registerProcessor(String id, URI uri) {
        boolean isNew;
        if (processors.containsKey(id)) {
            isNew = false;
            processors.get(id).lastSeen = Instant.now();
            processors.get(id).uris.add(uri);
        } else {
            isNew = true;
            ProcessorEntry entry = new ProcessorEntry(id, Instant.now(), uri);
            processors.put(id, entry);
        }
        listeners.parallelStream().forEach(listener -> listener.processorFound(id, uri, isNew));
    }
}
