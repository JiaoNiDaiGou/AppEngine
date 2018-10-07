package jiaoni.common.appengine.filters;

import jiaoni.common.appengine.auth.Authenticator;
import jiaoni.common.appengine.guice.Authenticators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Priority(FilterPriorities.AUTH)
@Provider
public class AuthFilter implements ContainerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);

    private final List<Authenticator> authenticators;

    @Context
    private HttpServletRequest servletRequest;

    @Inject
    public AuthFilter(@Authenticators final List<Authenticator> authenticators) {
        this.authenticators = authenticators;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        for (Authenticator authenticator : authenticators) {
            boolean authPass = authenticator.tryAuth(servletRequest, requestContext);
            LOGGER.info("{}fit {}", (authPass ? "" : "Not "), authenticator.getClass().getSimpleName());
            if (authPass) {
                return;
            }
        }
    }
}
