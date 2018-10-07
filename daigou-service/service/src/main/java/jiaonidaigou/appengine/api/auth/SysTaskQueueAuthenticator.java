package jiaonidaigou.appengine.api.auth;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;

import static jiaonidaigou.appengine.api.auth.AuthUtils.APP_ENGINE_CRON_HEADER_KEY;
import static jiaonidaigou.appengine.api.auth.AuthUtils.APP_ENGINE_QUEUE_NAME_HEADER_KEY;

/**
 * Authenticator for GAE task queue and cron jobs.
 */
public class SysTaskQueueAuthenticator implements Authenticator {
    @Override
    public boolean tryAuth(final HttpServletRequest request,
                           final ContainerRequestContext requestContext) {
        String name = requestContext.getHeaderString(APP_ENGINE_QUEUE_NAME_HEADER_KEY);
        if (StringUtils.isBlank(name)) {
            name = requestContext.getHeaderString(APP_ENGINE_CRON_HEADER_KEY);
        }
        if (StringUtils.isBlank(name)) {
            return false;
        }

        UserPrincipal principal = new UserPrincipal(
                requestContext.getHeaderString(APP_ENGINE_QUEUE_NAME_HEADER_KEY),
                UserPrincipal.AuthenticationScheme.GAE_TASK_QUEUE,
                true,
                Roles.SYS_TASK_QUEUE_OR_CRON);
        AuthUtils.updateUserPrinciple(requestContext, principal);
        return true;
    }
}
