package jiaonidaigou.appengine.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;

/**
 * A bypass authenticator, which directly assign given role.
 * It is used for local test ONLY.
 */
public class BypassAuthenticator implements Authenticator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BypassAuthenticator.class);

    private final String role;

    public BypassAuthenticator(final String role) {
        this.role = role;
    }

    @Override
    public boolean tryAuth(final HttpServletRequest request,
                           final ContainerRequestContext requestContext) {

        LOGGER.info("Bypass authentication. Directly set role to {} to access {}", role, requestContext.getUriInfo().getPath());

        UserPrincipal principal = new UserPrincipal(
                "bypass",
                UserPrincipal.AuthenticationScheme.GAE_TASK_QUEUE,
                true,
                role);
        AuthUtils.updateUserPrinciple(requestContext, principal);
        return true;
    }
}
