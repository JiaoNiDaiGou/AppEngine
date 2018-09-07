package jiaonidaigou.appengine.api.auth;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;

import static jiaonidaigou.appengine.api.auth.AuthUtils.APP_ENGINE_CRON_HEADER_KEY;
import static jiaonidaigou.appengine.api.auth.AuthUtils.APP_ENGINE_QUEUE_NAME_HEADER_KEY;

/**
 * Authenticator for GAE task queue and cron jobs.
 */
public class SysTaskQueueAuthenticator implements Authenticator {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuth2Authenticator.class);

    @Override
    public boolean canAuth(ContainerRequestContext requestContext) {
        return StringUtils.isNotBlank(requestContext.getHeaderString(APP_ENGINE_QUEUE_NAME_HEADER_KEY)) ||
                StringUtils.isNotBlank(requestContext.getHeaderString(APP_ENGINE_CRON_HEADER_KEY));
    }

    @Override
    public void auth(ContainerRequestContext requestContext) {
        String name = requestContext.getHeaderString(APP_ENGINE_QUEUE_NAME_HEADER_KEY);
        if (StringUtils.isBlank(name)) {
            name = requestContext.getHeaderString(APP_ENGINE_CRON_HEADER_KEY);
        }
        LOGGER.info("Found GAE taskqueue/cron header {}", name);
        UserPrincipal principal = new UserPrincipal(
                requestContext.getHeaderString(APP_ENGINE_QUEUE_NAME_HEADER_KEY),
                UserPrincipal.AuthenticationScheme.GAE_TASK_QUEUE,
                true,
                Role.SYS_TASK_QUEUE_OR_CRON);
        AuthUtils.updateUserPrinciple(requestContext, principal);
    }
}
