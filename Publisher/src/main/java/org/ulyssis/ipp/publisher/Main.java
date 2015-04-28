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

import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.snapshot.SnapshotListener;
import org.ulyssis.ipp.utils.Serialization;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> nextProcessingOfSnapshot;

    private final SnapshotListener snapshotListener;
    private final Thread snapshotThread;
    private final PublisherOptions options;

    private final LinkedBlockingQueue<Snapshot> snapshotQueue = new LinkedBlockingQueue<>();

    private final Thread httpThread;
    private final LinkedBlockingQueue<Snapshot> snapshotsToSubmit = new LinkedBlockingQueue<>();

    private Main(PublisherOptions options) {
        this.options = options;
        Runtime.getRuntime().addShutdownHook(new Thread(this::interruptHook));
        Config.setCurrentConfig(Config.fromConfigurationFile(options.getConfigFile()).get());
        snapshotListener = new SnapshotListener(options.getRedisUri());
        snapshotThread = new Thread(snapshotListener);
        snapshotListener.addListener(onSnapshot);
        snapshotListener.trigger();
        snapshotThread.start();
        if (options.getHttp() != null) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
                };

                // Install the all-trusting trust manager
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                // Create all-trusting host name verifier
                HostnameVerifier allHostsValid = new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };

                // Install the all-trusting host verifier
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            } catch (Exception e) {
                LOG.error("Problem registering all-trusting SSL manager");
            }

            httpThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            Snapshot snapshot = snapshotsToSubmit.take();
                            while (snapshotsToSubmit.size() > 0) {
                                // Skip ahead
                                snapshot = snapshotsToSubmit.take();
                            }
                            URL url = options.getHttp();
                            HttpURLConnection connection = null;
                            try {
                                connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("POST");
                                connection.setRequestProperty("Content-Type",
                                        "application/json");
                                String auth = url.getUserInfo();
                                if (auth != null) {
                                    String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                                    connection.setRequestProperty("Authorization", "Basic " + encoded);
                                }
                                connection.setUseCaches(false);
                                connection.setDoInput(true);
                                connection.setDoOutput(true);

                                DataOutputStream wr = new DataOutputStream(
                                        connection.getOutputStream()
                                );
                                Serialization.getJsonMapper().writeValue(wr, snapshot);
                                wr.flush();
                                wr.close();

                                InputStream is = connection.getInputStream();
                                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                                String line;
                                StringBuilder response = new StringBuilder();
                                while((line = rd.readLine()) != null) {
                                    LOG.debug(line);
                                }
                                rd.close();
                            } catch (Exception e) {
                                LOG.error("Error submitting JSON via HTTP", e);
                            } finally {
                                if (connection != null) {
                                    connection.disconnect();
                                }
                            }
                        }
                    } catch (InterruptedException ignored) {
                    }
                }
            });
            httpThread.start();
        } else {
            httpThread = null;
        }
        try {
            while (true) {
                Snapshot snapshot = snapshotQueue.take();
                service.submit(() -> processSnapshot(snapshot));
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void interruptHook() {
        LOG.info("Stopping Publisher");
        service.shutdownNow();
        snapshotListener.stop();
        httpThread.interrupt();
        LOG.info("Bye bye!");
        Configurator.shutdown((LoggerContext) LogManager.getContext());
    }

    private final Consumer<Snapshot> onSnapshot = snapshot -> snapshotQueue.add(snapshot);

    private void processSnapshot(Snapshot snapshot) {
        if (nextProcessingOfSnapshot != null) {
            nextProcessingOfSnapshot.cancel(false);
            nextProcessingOfSnapshot = null;
        }
        Score score = new Score(snapshot);
        try {
            File f = File.createTempFile("/tmp", ".json"); // TODO: better tmp path?
            Serialization.getJsonMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(f, score);
            Files.move(f.toPath(), options.getOutputFile(), StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Wrote new score for snapshot: {}", snapshot.getSnapshotTime());
            nextProcessingOfSnapshot = service.schedule(() -> processSnapshot(snapshot), 1L, TimeUnit.SECONDS);
        } catch (IOException e) {
            LOG.error("Error writing score", e);
        }
        if (options.getHttp() != null) {
            snapshotsToSubmit.add(snapshot);
        }
    }

    public static void main(String[] args) {
        Optional<PublisherOptions> optionsOptional = PublisherOptions.publisherOptionsFromArgs(args);
        if (optionsOptional.isPresent()) {
            PublisherOptions options = optionsOptional.get();
            new Main(options);
        } else {
            System.exit(-1);
        }
    }
}
