package jiaonidaigou.appengine.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;

// TODO
public class SelfAuthenticator implements Authenticator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelfAuthenticator.class);

    @Override
    public boolean tryAuth(final HttpServletRequest request, final ContainerRequestContext requestContext) {
        return false;
    }
}
