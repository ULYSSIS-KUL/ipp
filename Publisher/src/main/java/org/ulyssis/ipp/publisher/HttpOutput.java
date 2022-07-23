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
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.utils.Serialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class HttpOutput extends ScoreOutput {
    private static final Logger LOG = LogManager.getLogger(HttpOutput.class);
    private final PublisherOptions options;
    private final byte[] hmacKey;


    private CloseableHttpClient httpClient;

    /**
     * Score output that serializes scores to JSON and forwards them via a POST request.
     *
     * Can use a private key for client SSL authentication.
     */
    public HttpOutput(PublisherOptions options) throws IOException {
        this.options = options;
        hmacKey = Files.readAllBytes(options.getHmacKeyFilePath());
    }


    @Override
    public void outputScore(Score score) {
        if (httpClient == null)
            httpClient = HttpClients.createDefault();

        try {
            HttpPost req = new HttpPost(options.getHttp().toURI());
            byte[] scoreBytes = Serialization.getJsonMapper().writeValueAsBytes(score);
            HttpEntity ent = new ByteArrayEntity(scoreBytes, ContentType.APPLICATION_JSON);
            req.setEntity(ent);

            String hmac = Hmac.generateHmac(scoreBytes, hmacKey);
            req.setHeader("X-Hmac", hmac);

            try (CloseableHttpResponse response = httpClient.execute(req)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    LOG.error("Received non-success result! Status code was {} {}",
                            response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                    return;
                }
                HttpEntity entity = response.getEntity();
                if (entity.getContentLength() != 7L) {
                    LOG.error("Received response with wrong content length. Content length was {}",
                            entity.getContentLength());
                    return;
                }
                String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                if (!"SUCCESS".equals(result)) {
                    LOG.error("Did not receive \"SUCCESS\" response.");
                    return;
                }
            }
        } catch (Exception e) {
            LOG.error("An error occurred while POSTing score", e);
            try {
                httpClient.close();
            } catch (Exception e2) {
                LOG.error("An error occurred when closing connection", e2);
            }
            httpClient = null;
        }
    }

    @Override
    public void cleanup() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (Exception e) {
                LOG.error("An error occurred when closing connection", e);
            }
        }
    }
}
