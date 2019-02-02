package jiaoni.daigou.tools.locallauncher;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jiaoni.common.appengine.access.db.DoNothingMemcache;
import jiaoni.common.model.Env;
import jiaoni.daigou.service.appengine.ApiApplication;
import jiaoni.daigou.service.appengine.AppEnvs;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;

/**
 * Starts the service locally.
 */
public class LocalLauncher {
    public static void main(String[] args) {
        HttpServer server = null;

        try {
            String uri = StringUtils.substringBeforeLast(AppEnvs.getHostname(Env.LOCAL), ":");
            int port = Integer.parseInt(StringUtils.substringAfterLast(AppEnvs.getHostname(Env.LOCAL), ":"));

            URI baseUri = UriBuilder.fromUri(uri).port(port).build();
            ResourceConfig resourceConfig = new ApiApplication(new LocalModule());
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

    private static class LocalModule extends AbstractModule {
        @Override
        protected void configure() {
            super.configure();
        }

        @Provides
        @Singleton
        MemcacheService provideMemcacheService() {
            return new DoNothingMemcache();
        }
    }
}
