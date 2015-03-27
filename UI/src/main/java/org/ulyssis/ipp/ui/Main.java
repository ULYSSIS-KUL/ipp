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
package org.ulyssis.ipp.ui;

import eu.webtoolkit.jwt.ServletInit;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.LoginConfig;

import javax.servlet.ServletException;
import java.util.concurrent.Executors;

public final class Main {
    public static void main(String[] args) {
        UIOptions.uiOptionsFromArgs(args).ifPresent(options -> {
            try {
                DeploymentInfo servletBuilder = Servlets.deployment()
                        .setClassLoader(Main.class.getClassLoader())
                        .setContextPath("/ui")
                        .setDeploymentName("ipp-ui.war")
                        .addListener(new ListenerInfo(ServletInit.class))
                .addServlet(Servlets.servlet("UIServlet", UIServlet.class)
                        .addInitParam("config", options.getConfigFile().toString())
                        .setAsyncSupported(true)
                        .addMapping("/*"));

                DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
                manager.deploy();
                PathHandler path = Handlers.path(
                        Handlers.resource(new ClassPathResourceManager(Main.class.getClassLoader(), "WebRoot")))
                                .addExactPath("/", Handlers.redirect("/ui"))
                                .addPrefixPath("/ui", manager.start());

                Undertow server = Undertow.builder()
                        .addHttpListener(options.getPort(), options.getHost())
                        .setHandler(path)
                        .build();
                server.start();
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
