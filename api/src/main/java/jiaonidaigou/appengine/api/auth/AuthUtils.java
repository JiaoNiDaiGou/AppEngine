package jiaonidaigou.appengine.api.auth;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

class AuthUtils {

    // These headers are set internally by App Engine. If any of these headers are present in an external
    // user request to your app, they are stripped. If your request handler finds any of these headers,
    // it can be sure that it has received a valid task queue request.
    static final String APP_ENGINE_QUEUE_NAME_HEADER_KEY = "X-AppEngine-QueueName";
    static final String APP_ENGINE_CRON_HEADER_KEY = "X-AppEngine-Cron";
    static final String IP_ADDRESS_HEADER_KEY = "ip_address";
    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    private static final String BEARER_TOKEN_PREFIX = "Bearer ";

    static Optional<String> extractBearerToken(final ContainerRequestContext requestContext) {
        String token = requestContext.getHeaderString(AUTHORIZATION_HEADER_KEY);
        if (StringUtils.isBlank(token) || !token.startsWith(BEARER_TOKEN_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(token.substring(BEARER_TOKEN_PREFIX.length()));
    }

    static void updateUserPrinciple(final ContainerRequestContext requestContext, final UserPrincipal principal) {
        SecurityContext securityContext = new SecurityContext() {
            @Override
            public UserPrincipal getUserPrincipal() {
                return principal;
            }

            @Override
            public boolean isUserInRole(String roleStr) {
                return getUserPrincipal().getRoles().contains(Role.valueOf(roleStr));
            }

            @Override
            public boolean isSecure() {
                return principal.isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return principal.getScheme().name();
            }
        };
        requestContext.setSecurityContext(securityContext);
    }
}
