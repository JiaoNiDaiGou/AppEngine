package jiaonidaigou.appengine.api;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class ApiApplication extends ResourceConfig {
    private static final String ROOT_PACKAGE = "jiaonidaigou.appengine.api";

    public ApiApplication() {
        packages(ROOT_PACKAGE);
        Injector injector = Guice.createInjector(new ServiceModule());
        HK2toGuiceModule hk2Module = new HK2toGuiceModule(injector);
        register(hk2Module);
    }
}
