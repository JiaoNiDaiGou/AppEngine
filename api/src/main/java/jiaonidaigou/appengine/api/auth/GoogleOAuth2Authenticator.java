package jiaonidaigou.appengine.api.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.Oauth2.Builder;
import com.google.api.services.oauth2.model.Tokeninfo;
import com.google.common.collect.Sets;
import jiaoni.common.utils.Secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;

/**
 * Authenticator based on Google OAuth2.
 * See https://developers.google.com/identity/protocols/OAuth2
 */
public class GoogleOAuth2Authenticator implements Authenticator {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuth2Authenticator.class);

    private static final Set<String> ADMIN_EMAILS = Sets.newHashSet(Secrets.of("gae.admin.email").getAsStringLines());

    @Override
    public boolean tryAuth(final HttpServletRequest request,
                           final ContainerRequestContext requestContext) {
        Optional<String> token = AuthUtils.extractBearerToken(requestContext);
        if (!token.isPresent()) {
            return false;
        }

        Tokeninfo tokeninfo = getTokeninfo(token.get());
        if (tokeninfo == null) {
            return false;
        }

        String email = tokeninfo.getEmail();

        if (!ADMIN_EMAILS.contains(email.toLowerCase())) {
            LOGGER.error("email:{} is not trusted.", email);
            throw new ForbiddenException();
        }

        UserPrincipal principal = new UserPrincipal(
                email,
                UserPrincipal.AuthenticationScheme.GOOGLE_OAUTH2,
                requestContext.getSecurityContext().isSecure(),
                Roles.ADMIN);
        AuthUtils.updateUserPrinciple(requestContext, principal);
        return true;
    }

    private Tokeninfo getTokeninfo(final String token) {
        GoogleCredential credential = (new GoogleCredential()).setAccessToken(token);
        Oauth2 oauth2 = (new Builder(new NetHttpTransport(), new JacksonFactory(), credential))
                .setApplicationName("SongFan").build();

        try {
            HttpResponse resp = oauth2.tokeninfo().setAccessToken(token).executeUnparsed();
            if (resp.getStatusCode() >= 400 && resp.getStatusCode() < 500) {
                throw new ForbiddenException();
            } else {
                LOGGER.info("Got response with status: " + resp.getStatusCode());

                Tokeninfo tokeninfo;
                try {
                    tokeninfo = resp.parseAs(Tokeninfo.class);
                } catch (IllegalArgumentException var9) {
                    return null;
                }

                if (tokeninfo.containsKey("error")) {
                    LOGGER.warn("Error validating OAuth2 token: " + tokeninfo.get("error"));
                    return null;
                }
                return tokeninfo;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
