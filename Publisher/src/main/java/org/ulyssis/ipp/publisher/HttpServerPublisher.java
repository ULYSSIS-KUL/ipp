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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.utils.Serialization;

import java.io.*;
import java.nio.file.Files;
import java.util.Optional;

/**
 * A publisher that uses HTTP POST requests as the source.
 */
public final class HttpServerPublisher extends Publisher implements Runnable {
    private static final Logger LOG = LogManager.getLogger(HttpServerPublisher.class);

    private final Undertow server;
    private final PublisherOptions options;
    private final byte[] hmacKey;

    public HttpServerPublisher(PublisherOptions options) throws IOException {
        super(options);
        this.options = options;
        hmacKey = Files.readAllBytes(options.getHmacKeyFilePath());
        server = Undertow.builder()
                .addHttpListener(options.getPort(), options.getHost(), handler())
                .setWorkerThreads(10)
                .build();
    }

    private static final class InfoHandler implements HttpHandler {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            exchange.getResponseHeaders().put(HttpString.tryFromString("Content-Type"), "text/plain");
            if (exchange.getRequestMethod().equalToString("GET")) {
                if (exchange.getSecurityContext() != null) {
                    LOG.info("Authenticated: {}", exchange.getSecurityContext().isAuthenticated());
                }
                exchange.setResponseCode(200);
                exchange.getResponseSender().send("Howdy!");
            } else {
                exchange.setResponseCode(400);
                exchange.getResponseSender().send("Wrong request method!");
            }
        }
    }

    private final class UpdateHandler implements HttpHandler {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            if (exchange.isInIoThread()) {
                exchange.dispatch(this);
                return;
            }
            exchange.getResponseHeaders().put(HttpString.tryFromString("Content-Type"), "text/plain");
            if (exchange.getRequestMethod().equalToString("POST")) {
                String contentType = Optional.ofNullable(exchange.getRequestHeaders().get("Content-Type")).map(HeaderValues::getFirst).orElse(null);
                if (contentType != null && contentType.startsWith("application/json")) {
                    try {
                        String mac = exchange.getRequestHeaders().getFirst("X-Hmac");

                        exchange.startBlocking();

                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        InputStream is = exchange.getInputStream();
                        for (int length; (length = is.read(buffer)) != -1; ) {
                            os.write(buffer, 0, length);
                        }
                        byte[] body = os.toByteArray();

                        if (Hmac.verifyHmac(mac, body, hmacKey)) {
                            Score score = Serialization.getJsonMapper().readValue(body, Score.class);
                            outputScore(score);
                            exchange.setResponseCode(200);
                            exchange.getResponseSender().send("SUCCESS");
                        } else {
                            exchange.setResponseCode(400);
                            exchange.getResponseSender().send("Invalid HMAC");
                        }
                    } catch (JsonParseException e) {
                        LOG.error("Couldn't parse JSON", e);
                        exchange.setResponseCode(400);
                        exchange.getResponseSender().send("Badly formatted JSON!");
                    } catch (JsonMappingException e) {
                        LOG.error("Couldn't map JSON to Score object", e);
                        exchange.setResponseCode(400);
                        exchange.getResponseSender().send("JSON is not a valid Score");
                    } catch (IOException e) {
                        LOG.error("Unknown IO exception", e);
                        exchange.setResponseCode(400);
                        exchange.getResponseSender().send("Unknown IO error");
                    }
                } else {
                    exchange.setResponseCode(400);
                    exchange.getResponseSender().send("JSON content type required!");
                }
            } else {
                exchange.setResponseCode(400);
                exchange.getResponseSender().send("Wrong request method!");
            }
        }
    }


    private HttpHandler handler() {
        return Handlers.path()
                .addExactPath("/", new InfoHandler())
                .addExactPath("/update", new UpdateHandler());
    }

    @Override
    public void run() {
        LOG.info("Starting Undertow!");
        Thread thread = new Thread(server::start);
        thread.start();
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(10L);
            }
        } catch (InterruptedException ignored) {}
        server.stop();
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException ignored) {}
        cleanup();
    }
}
