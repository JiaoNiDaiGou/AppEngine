package jiaoni.common.appengine.auth;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import jiaoni.common.appengine.access.db.DbClient;
import jiaoni.common.model.Env;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;

import static jiaoni.common.appengine.auth.AuthUtils.WX_SESSION_TICKET_HEADER_KEY;

public class WxAuthenticator implements Authenticator {
    private static final Logger LOGGER = LoggerFactory.getLogger(WxAuthenticator.class);

    private static final long REFRESH_EXPIRATION_MILLIS = Duration.standardMinutes(5).getMillis();

    private final DbClient<WxSessionTicket> dbClient;

    public WxAuthenticator(final String serviceName, final Env env) {
        this.dbClient = new WxSessionDbClient(
                serviceName,
                env,
                DatastoreServiceFactory.getDatastoreService());
    }

    @Override
    public boolean tryAuth(final HttpServletRequest request,
                           final ContainerRequestContext requestContext) {

        String wxSessionTicketId = requestContext.getHeaderString(WX_SESSION_TICKET_HEADER_KEY);
        if (StringUtils.isBlank(wxSessionTicketId)) {
            return false;
        }

        WxSessionTicket ticket = dbClient.getById(wxSessionTicketId);
        LOGGER.info("load wx ticket {}", ticket);

        DateTime now = DateTime.now();
        if (ticket == null || ticket.getExpirationTime() == null || now.isAfter(ticket.getExpirationTime())) {
            throw new ForbiddenException();
        }

        // Refresh ticket expiration time if needed.
        if (now.isAfter(ticket.getExpirationTime().minus(REFRESH_EXPIRATION_MILLIS))) {
            ticket = ticket.toBuilder()
                    .withExpirationTime(now.plus(WxSessionTicket.DEFAULT_EXPIRATION_MILLIS))
                    .build();
            dbClient.put(ticket);
        }

        UserPrincipal principal = new UserPrincipal(
                ticket.getOpenId(),
                UserPrincipal.AuthenticationScheme.CUSTOM_SECRET,
                requestContext.getSecurityContext().isSecure(),
                Roles.ADMIN);
        AuthUtils.updateUserPrinciple(requestContext, principal);
        return true;
    }
}
