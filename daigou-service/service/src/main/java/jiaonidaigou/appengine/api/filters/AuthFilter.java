package jiaonidaigou.appengine.api.filters;

import com.google.common.collect.Lists;
import jiaonidaigou.appengine.api.auth.Authenticator;
import jiaonidaigou.appengine.api.auth.BypassAuthenticator;
import jiaonidaigou.appengine.api.auth.CustomSecretAuthenticator;
import jiaonidaigou.appengine.api.auth.GoogleOAuth2Authenticator;
import jiaonidaigou.appengine.api.auth.SysTaskQueueAuthenticator;
import jiaonidaigou.appengine.api.auth.WxAuthenticator;
import jiaonidaigou.appengine.api.utils.AppEnvironments;
import jiaoni.common.model.Env;
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
    public AuthFilter() {
        // Order matters, is the order to try authentication.
        if (AppEnvironments.ENV == Env.LOCAL) {
            authenticators = Lists.newArrayList(
                    new BypassAuthenticator()
            );
        } else {
            authenticators = Lists.newArrayList(
                    new SysTaskQueueAuthenticator(),
                    new WxAuthenticator(),
                    new CustomSecretAuthenticator(),
                    new GoogleOAuth2Authenticator()
            );
        }
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