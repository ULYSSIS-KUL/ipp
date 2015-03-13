package org.ulyssis.ipp.processor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Objects;
import java.util.UUID;

public final class ProcessorAnnouncer implements Runnable {
    private static final Logger LOG = LogManager.getLogger(ProcessorAnnouncer.class);

    private int port;
    private String announceString;
    private URI redisUri;
    private InetAddress localAddress;
    private DatagramSocket socket;

    public ProcessorAnnouncer(int port, String announceString, URI redisUri) {
        this.port = port;
        this.announceString = announceString + "\r\n" + UUID.randomUUID().toString();
        this.redisUri = redisUri;
        try {
            this.localAddress = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            LOG.error("Couldn't find conventional address?", e);
        }
        try {
            this.socket = new DatagramSocket();
            this.socket.setBroadcast(true);
            this.socket.setReuseAddress(true);
        } catch (IOException e) {
            LOG.error("Couldn't create socket?", e);
        }
    }

    private byte[] buildBroadCastString(InetAddress sourceAddress) {
        URI uri = redisUri;
        try {
            String hostAddress = uri.getHost();
            if (Objects.equals(hostAddress, "127.0.0.1")) {
                hostAddress = sourceAddress.getHostAddress();
            }
            uri = new URI(uri.getScheme(), null, hostAddress,
                    uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
            return (announceString + "\r\n" + uri.toASCIIString() + "\r\n").getBytes(StandardCharsets.US_ASCII);
        } catch (URISyntaxException e) {
            LOG.error("Couldn't properly contruct URI?", e);
        }
        return new byte[0];
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                broadcast();
                Thread.sleep(5000L);
            }
        } catch (InterruptedException ignored) {
        }
    }

    public void broadcast() {
        byte[] broadCastString = buildBroadCastString(localAddress);
        DatagramPacket packet = new DatagramPacket(broadCastString, broadCastString.length, localAddress, port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            LOG.error("Couldn't send announce packet?", e);
        }
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp())
                    continue;

                for (InterfaceAddress ifAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress address = ifAddress.getAddress();
                    InetAddress bcAddress = ifAddress.getBroadcast();
                    if (bcAddress == null)
                        continue;

                    broadCastString = buildBroadCastString(address);
                    packet = new DatagramPacket(broadCastString, broadCastString.length, bcAddress, port);
                    try {
                        socket.send(packet);
                    } catch (IOException e) {
                        LOG.error("Couldn't send announce packet?", e);
                    }
                }
            }
        } catch (SocketException e) {
            LOG.error("Error getting network interfaces", e);
        }
    }
}
