package jiaonidaigou.appengine.api;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.inject.Guice;
import com.google.inject.Injector;
import jiaoni.common.appengine.filters.AuthFilter;
import jiaoni.common.appengine.filters.CorsFilter;
import jiaoni.common.appengine.filters.WireLogFilter;
import jiaoni.common.appengine.guice.HK2toGuiceModule;
import jiaoni.common.appengine.utils.ExceptionMappingFeature;
import jiaoni.common.appengine.utils.ObjectMapperContextResolver;
import jiaonidaigou.appengine.api.guice.ServiceModule;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class ApiApplication extends ResourceConfig {
    private static final String ROOT_PACKAGE = "jiaonidaigou.appengine.api";
    private static final String INTERFACE_PACKAGE = ROOT_PACKAGE + ".interfaces";

    public ApiApplication() {
        register(JacksonJaxbJsonProvider.class);
        register(ObjectMapperContextResolver.class);
        register(RolesAllowedDynamicFeature.class);
        register(ExceptionMappingFeature.class);
        register(MultiPartFeature.class);

        register(AuthFilter.class);
        register(CorsFilter.class);
        register(WireLogFilter.class);

        packages(ROOT_PACKAGE);
        Injector injector = Guice.createInjector(new ServiceModule());
        HK2toGuiceModule hk2Module = new HK2toGuiceModule(ROOT_PACKAGE, injector);
        register(hk2Module);
    }
}
