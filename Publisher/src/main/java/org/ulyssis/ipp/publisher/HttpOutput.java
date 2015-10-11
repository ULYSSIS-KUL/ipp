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

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.utils.Serialization;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

public final class HttpOutput implements ScoreOutput {
    private static final Logger LOG = LogManager.getLogger(HttpOutput.class);
    private final PublisherOptions options;

    public HttpOutput(PublisherOptions options) {
        this.options = options;
    }

    private SSLContext createSslCustomContext() {
        try {
            SSLContextBuilder builder = SSLContexts.custom();
            if (options.getKeystore().isPresent()) {
                KeyStore cks = KeyStore.getInstance(KeyStore.getDefaultType());
                cks.load(new FileInputStream(options.getKeystore().get().toFile()), options.getKeystorePass().toCharArray());
                builder.loadKeyMaterial(cks, options.getKeystorePass().toCharArray());
            }

            if (options.getTruststore().isPresent()) {
                KeyStore tks = KeyStore.getInstance(KeyStore.getDefaultType());
                tks.load(new FileInputStream(options.getTruststore().get().toFile()), options.getTruststorePass().toCharArray());
                builder.loadTrustMaterial(tks, new TrustSelfSignedStrategy());
            }

            if (!options.getKeystore().isPresent() && !options.getKeystore().isPresent()) {
                return SSLContext.getDefault();
            }

            return builder.build();
        } catch (Exception e) {
            // TODO: DO SOMETHING WITH THE EXCEPTION!
            LOG.error("Exception", e);
        }
        return null;
    }

    @Override
    public void outputScore(Score score) {
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(
                createSslCustomContext(),
                new String[]{"TLSv1"},
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());
        try (CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build()) {
            HttpPost req = new HttpPost(options.getHttp().toURI());
            byte[] scoreBytes = Serialization.getJsonMapper().writeValueAsBytes(score);
            HttpEntity ent = new ByteArrayEntity(scoreBytes, ContentType.APPLICATION_JSON);
            req.setEntity(ent);
            try (CloseableHttpResponse response = httpClient.execute(req)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    LOG.error("Non-success result!");
                    return;
                }
                HttpEntity entity = response.getEntity();
                if (entity.getContentLength() != 7L) {
                    LOG.error("Non-success result!");
                    return;
                }
                String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                if (!"SUCCESS".equals(result)) {
                    LOG.error("Non-success result!");
                    return;
                }
            }
        } catch (Exception e) {
            // TODO: DO SOMETHING WITH THE EXCEPTION!
            LOG.error("Exception", e);
        }
    }

    @Override
    public void cleanup() {
        // TODO: CLEANUP!
    }
}
