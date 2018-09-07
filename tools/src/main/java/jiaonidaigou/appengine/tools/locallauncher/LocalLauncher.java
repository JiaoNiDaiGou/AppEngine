package jiaonidaigou.appengine.tools.locallauncher;

import jiaonidaigou.appengine.api.ApiApplication;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import javax.ws.rs.core.UriBuilder;

/**
 * Starts the service locally.
 */
public class LocalLauncher {
    private static final String HOST = "http://localhost";
    private static final int PORT = 33256;

    public static void main(String[] args) throws Exception {
        HttpServer server = null;
        try {
            URI baseUri = UriBuilder.fromUri(HOST).port(PORT).build();
            ResourceConfig resourceConfig = new ApiApplication();
            server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);
            server.start();
            System.out.println("Press enter to stop the server...");
            Runtime.getRuntime().exec(new String[]{ "open", HOST + ":" + PORT });
            System.in.read();
        } finally {
            if (server != null && server.isStarted()) {
                server.shutdownNow();
            }
        }
    }
}
