package jiaonidaigou.appengine.api.auth;

import javax.ws.rs.container.ContainerRequestContext;

/**
 * Interface of authenticator.
 */
public interface Authenticator {
    /**
     * Try auth the request.
     *
     * @param requestContext request.
     * @return True when auth pass. False when this authenticator cannot auth. throw ForbiddenException when auth failed.
     */
    boolean tryAuth(final ContainerRequestContext requestContext);
}
