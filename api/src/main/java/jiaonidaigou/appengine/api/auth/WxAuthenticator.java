package jiaonidaigou.appengine.api.auth;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import jiaonidaigou.appengine.api.access.db.WxSessionDbClient;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;

import static jiaonidaigou.appengine.api.auth.AuthUtils.WX_SESSION_TICKET_HEADER_KEY;

public class WxAuthenticator implements Authenticator {

    private final WxSessionDbClient dbClient;

    public WxAuthenticator() {
        dbClient = new WxSessionDbClient(DatastoreServiceFactory.getDatastoreService());
    }

    @Override
    public boolean tryAuth(final HttpServletRequest request,
                           final ContainerRequestContext requestContext) {

        String wxSessionTicketId = requestContext.getHeaderString(WX_SESSION_TICKET_HEADER_KEY);
        if (StringUtils.isBlank(wxSessionTicketId)) {
            return false;
        }

        WxSessionTicket ticket = dbClient.getById(wxSessionTicketId);
        if (ticket == null) {
            throw new ForbiddenException();
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
