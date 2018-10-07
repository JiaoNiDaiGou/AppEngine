package jiaoni.daigou.service.appengine;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import jiaoni.common.appengine.auth.Authenticator;
import jiaoni.common.appengine.auth.BypassAuthenticator;
import jiaoni.common.appengine.auth.CustomSecretAuthenticator;
import jiaoni.common.appengine.auth.GoogleOAuth2Authenticator;
import jiaoni.common.appengine.auth.SysTaskQueueAuthenticator;
import jiaoni.common.appengine.auth.WxAuthenticator;
import jiaoni.common.appengine.filters.AuthFilter;
import jiaoni.common.appengine.filters.CorsFilter;
import jiaoni.common.appengine.filters.WireLogFilter;
import jiaoni.common.appengine.guice.Authenticators;
import jiaoni.common.appengine.guice.HK2toGuiceModule;
import jiaoni.common.appengine.utils.ExceptionMappingFeature;
import jiaoni.common.appengine.utils.ObjectMapperContextResolver;
import jiaoni.common.model.Env;
import jiaoni.daigou.service.appengine.guice.ServiceModule;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import java.util.List;
import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class ApiApplication extends ResourceConfig {
    private static final String[] PACKAGES = {
            "jiaoni.common.appengine",
            "jiaoni.daigou.service"
    };
    private static final String INTERFACE_PACKAGE = "jiaoni.daigou.service.appengine.interfaces";

    public ApiApplication() {
        register(JacksonJaxbJsonProvider.class);
        register(ObjectMapperContextResolver.class);
        register(RolesAllowedDynamicFeature.class);
        register(ExceptionMappingFeature.class);
        register(MultiPartFeature.class);

        // Bind those Guice cannot bind
        register(new Binder());

        register(AuthFilter.class);
        register(CorsFilter.class);
        register(WireLogFilter.class);

        packages(PACKAGES);
        Injector injector = Guice.createInjector(new ServiceModule());
        HK2toGuiceModule hk2Module = new HK2toGuiceModule(INTERFACE_PACKAGE, injector);
        register(hk2Module);
    }

    private static class Binder extends AbstractBinder {
        @Override
        protected void configure() {
            List<Authenticator> authenticators;
            if (AppEnvs.getEnv() == Env.LOCAL) {
                authenticators = Lists.newArrayList(new BypassAuthenticator());
            } else {
                authenticators = Lists.newArrayList(
                        new SysTaskQueueAuthenticator(),
                        new WxAuthenticator(AppEnvs.getServiceName(), AppEnvs.getEnv()),
                        new CustomSecretAuthenticator(),
                        new GoogleOAuth2Authenticator());
            }
            bind(authenticators)
                    .to((new TypeLiteral<List<Authenticator>>() {
                    })).qualifiedBy(new Authenticators.Impl());
        }
    }
}
