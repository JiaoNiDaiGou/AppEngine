package jiaonidaigou.appengine.api.auth;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import jiaonidaigou.appengine.api.access.db.WxSessionDbClient;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;

import static jiaonidaigou.appengine.api.auth.AuthUtils.WX_SESSION_TICKET_HEADER_KEY;

public class WxAuthenticator implements Authenticator {
    private static final long REFRESH_EXPIRATION_MILLIS = Duration.standardMinutes(5).getMillis();

    private final WxSessionDbClient dbClient;

    public WxAuthenticator() {
        dbClient = new WxSessionDbClient(
                DatastoreServiceFactory.getDatastoreService(),
                MemcacheServiceFactory.getMemcacheService());
    }

    @Override
    public boolean tryAuth(final HttpServletRequest request,
                           final ContainerRequestContext requestContext) {

        String wxSessionTicketId = requestContext.getHeaderString(WX_SESSION_TICKET_HEADER_KEY);
        if (StringUtils.isBlank(wxSessionTicketId)) {
            return false;
        }

        WxSessionTicket ticket = dbClient.getById(wxSessionTicketId);
        DateTime now = DateTime.now();
        if (ticket == null || ticket.getExpirationTime() == null || now.isAfter(ticket.getExpirationTime())) {
            throw new ForbiddenException();
        }

        // Refresh ticket expireation time if needed.
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
