package jiaonidaigou.appengine.api.auth;

import javax.ws.rs.container.ContainerRequestContext;

/**
 * Interface of authenticator.
 */
public interface Authenticator {

    /**
     * Whether this authenticator can authorize the request.
     */
    boolean canAuth(final ContainerRequestContext requestContext);

    /**
     * Authorize the request.
     */
    void auth(final ContainerRequestContext requestContext);
}
