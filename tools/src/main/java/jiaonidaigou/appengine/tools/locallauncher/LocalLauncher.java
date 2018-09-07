package jiaonidaigou.appengine.tools.locallauncher;

import jiaonidaigou.appengine.api.ApiApplication;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import javax.ws.rs.core.UriBuilder;

public class LocalLauncher {
    private static final String HOST = "http://localhost";
    private static final int PORT = 33256;

    public static void main(String[] args) throws IOException {
        URI baseUri = UriBuilder.fromUri(HOST).port(PORT).build();
        ResourceConfig resourceConfig = new ApiApplication();
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);
        System.out.println("Press enter to stop the server...");
        Runtime.getRuntime().exec(new String[]{ "open", HOST + ":" + PORT });
        System.in.read();
    }
}
