package org.ulyssis.ipp.ui;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

import javax.servlet.ServletException;

public class Main {
    public static void main(String[] args) {
        // TODO: Parse all of the options here!
        try {
            DeploymentInfo servletBuilder = Servlets.deployment()
                    .setClassLoader(Main.class.getClassLoader())
                    .setContextPath("/ui")
                    .setDeploymentName("ipp-ui.war")
                    .addServlet(Servlets.servlet("UIServlet", UIServlet.class)
                            .addMapping("/*"));

            DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
            manager.deploy();
            PathHandler path = Handlers.path(Handlers.redirect("/ui"))
                    .addPrefixPath("/ui", manager.start());

            Undertow server = Undertow.builder()
                    .addHttpListener(8080, "localhost")
                    .setHandler(path)
                    .build();
            server.start();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }
}
