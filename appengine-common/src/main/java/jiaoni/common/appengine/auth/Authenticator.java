package jiaoni.common.appengine.auth;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;

/**
 * Interface of authenticator.
 */
public interface Authenticator {
    /**
     * Try auth the request.
     *
     * @param request        request
     * @param requestContext request context.
     * @return True when auth pass. False when this authenticator cannot auth. throw ForbiddenException when auth failed.
     */
    boolean tryAuth(final HttpServletRequest request, final ContainerRequestContext requestContext);
}
