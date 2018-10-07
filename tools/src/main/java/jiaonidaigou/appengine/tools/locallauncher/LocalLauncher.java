package jiaonidaigou.appengine.tools.locallauncher;

import jiaonidaigou.appengine.api.ApiApplication;
import jiaoni.common.utils.Environments;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import javax.ws.rs.core.UriBuilder;

/**
 * Starts the service locally.
 */
public class LocalLauncher {
    public static void main(String[] args) {
        HttpServer server = null;

        try {
            String uri = StringUtils.substringBeforeLast(Environments.LOCAL_ENDPOINT, ":");
            int port = Integer.parseInt(StringUtils.substringAfterLast(Environments.LOCAL_ENDPOINT, ":"));

            URI baseUri = UriBuilder.fromUri(uri).port(port).build();
            ResourceConfig resourceConfig = new ApiApplication();
            server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);
            server.start();
            System.out.println("Service started. Listened to port " + port + "...");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("Exited!")));

            while (true) {
                Thread.sleep(1000L);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (server != null && server.isStarted()) {
                System.out.println("Shutdown server");
                server.shutdownNow();
            }
        }
    }
}
