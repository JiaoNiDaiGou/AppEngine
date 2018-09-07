package jiaonidaigou.appengine.api.filters;

import com.google.common.collect.Lists;
import jiaonidaigou.appengine.api.auth.Authenticator;
import jiaonidaigou.appengine.api.auth.GoogleOAuth2Authenticator;
import jiaonidaigou.appengine.api.auth.SysTaskQueueAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

@Priority(FilterPriorities.AUTH)
public class AuthFilter implements ContainerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);

    private List<Authenticator> authenticators;

    @Inject
    public AuthFilter() {
        // Order matters, is the order to try authentication.
        authenticators = Lists.newArrayList(
                new SysTaskQueueAuthenticator(),
                new GoogleOAuth2Authenticator()
        );
    }

    @Override
    public void filter(final ContainerRequestContext requestContext)
            throws IOException {
        for (Authenticator authenticator : authenticators) {
            if (authenticator.canAuth(requestContext)) {
                LOGGER.info("Fit {}.", authenticator.getClass().getSimpleName());
                authenticator.auth(requestContext);
                return;
            } else {
                LOGGER.info("Not fit {}.", authenticator.getClass().getSimpleName());
            }
        }
    }
}
