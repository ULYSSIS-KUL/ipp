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
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.ClientCertAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.utils.Serialization;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.Principal;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * A publisher that uses HTTP POST requests as the source.
 */
public final class HttpServerPublisher extends Publisher implements Runnable {
    private static final Logger LOG = LogManager.getLogger(HttpServerPublisher.class);

    private final Undertow server;
    private final PublisherOptions options;

    public HttpServerPublisher(PublisherOptions options) {
        super(options);
        this.options = options;
        if (options.getKeystore().isPresent()) {
            server = Undertow.builder()
                    .addHttpsListener(options.getPort(), "0.0.0.0", sslContext(), handler())
                    .setWorkerThreads(10)
                    .build();
        } else {
            server = Undertow.builder()
                    .addHttpListener(options.getPort(), "0.0.0.0", handler())
                    .setWorkerThreads(10)
                    .build();
        }
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
                        exchange.startBlocking();
                        Score score = Serialization.getJsonMapper().readValue(exchange.getInputStream(), Score.class);
                        outputScore(score);
                        exchange.setResponseCode(200);
                        exchange.getResponseSender().send("SUCCESS");
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

    private SSLContext sslContext() {
        try {
            KeyStore cks = KeyStore.getInstance(KeyStore.getDefaultType());
            cks.load(new FileInputStream(options.getKeystore().get().toFile()),
                    options.getKeystorePass().toCharArray());
            SSLContextBuilder builder = SSLContexts.custom();
            if (options.getTruststore().isPresent()) {
                KeyStore tks = KeyStore.getInstance(KeyStore.getDefaultType());
                tks.load(new FileInputStream(options.getTruststore().get().toFile()),
                        options.getTruststorePass().toCharArray());
                builder.loadTrustMaterial(tks, new TrustSelfSignedStrategy());
            }
            return builder
                    .loadKeyMaterial(cks, options.getKeystorePass().toCharArray())
                    .build();
        } catch (Exception e) {
            // TODO: DO SOMETHING WITH THE EXCEPTION!
            LOG.error("Exception", e);
        }
        return null;
    }

    private static IdentityManager identityManager = new IdentityManager() {
        private final Principal principal = () -> "ROOT";

        @Override
        public Account verify(Account account) {
            return null;
        }

        @Override
        public Account verify(String id, Credential credential) {
            return null;
        }

        @Override
        public Account verify(Credential credential) {
            return new Account() {
                @Override
                public Principal getPrincipal() {
                    return principal;
                }

                @Override
                public Set<String> getRoles() {
                    return Collections.emptySet();
                }
            };
        }
    };

    private HttpHandler handler() {
        HttpHandler handler = Handlers.path()
                .addExactPath("/", new InfoHandler())
                .addExactPath("/update", new UpdateHandler());
        if (options.getKeystore().isPresent() && options.getTruststore().isPresent()) {
            return new SecurityInitialHandler(AuthenticationMode.CONSTRAINT_DRIVEN, identityManager,
                new AuthenticationMechanismsHandler(
                        new AuthenticationConstraintHandler(
                                new AuthenticationCallHandler(handler)),
                        Collections.singletonList(new ClientCertAuthenticationMechanism())));
        } else {
            return handler;
        }
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
