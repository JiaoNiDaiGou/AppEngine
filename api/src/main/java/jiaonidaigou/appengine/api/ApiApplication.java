package jiaonidaigou.appengine.api;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.inject.Guice;
import com.google.inject.Injector;
import jiaonidaigou.appengine.api.filters.AuthFilter;
import jiaonidaigou.appengine.api.filters.CorsFilter;
import jiaonidaigou.appengine.api.filters.WireLogFilter;
import jiaonidaigou.appengine.api.guice.HK2toGuiceModule;
import jiaonidaigou.appengine.api.guice.ServiceModule;
import jiaonidaigou.appengine.api.utils.ExceptionMappingFeature;
import jiaonidaigou.appengine.api.utils.ObjectMapperContextResolver;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class ApiApplication extends ResourceConfig {
    private static final String ROOT_PACKAGE = "jiaonidaigou.appengine.api";

    public ApiApplication() {
        register(JacksonJaxbJsonProvider.class);
        register(ObjectMapperContextResolver.class);
        register(RolesAllowedDynamicFeature.class);
        register(ExceptionMappingFeature.class);

        register(AuthFilter.class);
        register(CorsFilter.class);
        register(WireLogFilter.class);

        packages(ROOT_PACKAGE);
        Injector injector = Guice.createInjector(new ServiceModule());
        HK2toGuiceModule hk2Module = new HK2toGuiceModule(injector);
        register(hk2Module);
    }
}
