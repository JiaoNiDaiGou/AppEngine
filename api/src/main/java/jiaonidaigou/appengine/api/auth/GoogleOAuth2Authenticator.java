package jiaonidaigou.appengine.api.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.Oauth2.Builder;
import com.google.api.services.oauth2.model.Tokeninfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;

import static jiaonidaigou.appengine.api.auth.AuthUtils.extractBearerToken;

/**
 * Authenticator based on Google OAuth2.
 * See https://developers.google.com/identity/protocols/OAuth2
 */
public class GoogleOAuth2Authenticator implements Authenticator {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuth2Authenticator.class);

    private static final String ADMIN_EMAIL = "songfan.rfu@gmail.com";

    @Override
    public boolean canAuth(ContainerRequestContext requestContext) {
        return extractBearerToken(requestContext).isPresent();
    }

    @Override
    public void auth(final ContainerRequestContext requestContext) {
        String token = extractBearerToken(requestContext)
                .orElseThrow(() -> new IllegalStateException("bearer token cannot be blank."));
        Tokeninfo tokeninfo = getTokeninfo(token);
        String email = tokeninfo.getEmail();
        if (!ADMIN_EMAIL.equalsIgnoreCase(email)) {
            throw new ForbiddenException();
        }
        UserPrincipal principal = new UserPrincipal(
                email,
                UserPrincipal.AuthenticationScheme.GOOGLE_OAUTH2,
                requestContext.getSecurityContext().isSecure(),
                Role.ADMIN);
        AuthUtils.updateUserPrinciple(requestContext, principal);
    }

    private Tokeninfo getTokeninfo(final String token) {
        GoogleCredential credential = (new GoogleCredential()).setAccessToken(token);
        Oauth2 oauth2 = (new Builder(new NetHttpTransport(), new JacksonFactory(), credential))
                .setApplicationName("jiaonidaigou").build();

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
                    throw new ForbiddenException();
                }

                if (tokeninfo.containsKey("error")) {
                    LOGGER.warn("Error validating OAuth2 token: " + tokeninfo.get("error"));
                    throw new ForbiddenException();
//                } else if (!googleOpenIdClientIds.contains(tokeninfo.getIssuedTo())) {
//                    LOGGER.warn("Unsupported issuer [" + tokeninfo.getIssuedTo() + "] for supplied token.");
//                    throw new ForbiddenException();
                } else {
                    return tokeninfo;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
