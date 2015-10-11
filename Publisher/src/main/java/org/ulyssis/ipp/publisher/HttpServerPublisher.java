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

import sabina.Request;
import sabina.Server;

/**
 * A publisher that uses HTTP POST requests as the source.
 */
public final class HttpServerPublisher extends Publisher implements Runnable {
    private final Server server;

    public HttpServerPublisher(PublisherOptions options) {
        super(options);
        // TODO: THIS IS A STUB!
        server = new Server(options.getPort());
        server.secure(null, null, null, null); // TODO: Keystore file!
        server.post("/update", this::updateHandler);
        throw new UnsupportedOperationException();
    }

    public Object updateHandler(Request request) {
        return "SUCCESS";
    }

    @Override
    public void run() {
        // TODO: THIS IS A STUB!
        server.start();
        throw new UnsupportedOperationException();
    }
}
