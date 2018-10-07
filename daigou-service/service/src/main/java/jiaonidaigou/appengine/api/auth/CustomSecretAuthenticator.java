package jiaonidaigou.appengine.api.auth;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import jiaoni.common.utils.EncryptUtils;
import jiaoni.common.utils.Secrets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;

import static jiaonidaigou.appengine.api.auth.AuthUtils.CUSTOM_SECRET_HEADER_KEY;

public class CustomSecretAuthenticator implements Authenticator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomSecretAuthenticator.class);

    private static final Set<String> ADMIN_WE_USER_IDS = Sets.newHashSet(Secrets.of("gae.admin.weUserId").getAsStringLines());

    private static final byte[] SERVER_KEY;
    private static final byte[] SERVER_IV;

    static {
        String[] lines = Secrets.of("gae.global.keyAndIv").getAsStringLines();
        SERVER_KEY = Base64.getDecoder().decode(lines[0].getBytes(Charsets.UTF_8));
        SERVER_IV = Base64.getDecoder().decode(lines[1].getBytes(Charsets.UTF_8));
    }

    @Override
    public boolean tryAuth(final HttpServletRequest request,
                           final ContainerRequestContext requestContext) {
        String customSecretHeader = requestContext.getHeaderString(CUSTOM_SECRET_HEADER_KEY);
        if (StringUtils.isBlank(customSecretHeader)) {
            return false;
        }

        String weUserId;
        try {
            // header -> base64.decode -> AES.decrypt -> string;
            byte[] bytes = Base64.getDecoder().decode(customSecretHeader.getBytes(Charsets.UTF_8));
            bytes = EncryptUtils.aesDecrypt(SERVER_KEY, SERVER_IV, bytes);
            weUserId = new String(bytes, Charsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("Failed to decrypt custom secret header", e);
            throw new ForbiddenException();
        }

        if (!ADMIN_WE_USER_IDS.contains(weUserId)) {
            LOGGER.error("weUserId:{} is not trusted.", weUserId);
            throw new ForbiddenException();
        }

        UserPrincipal principal = new UserPrincipal(
                weUserId,
                UserPrincipal.AuthenticationScheme.CUSTOM_SECRET,
                requestContext.getSecurityContext().isSecure(),
                Roles.ADMIN);
        AuthUtils.updateUserPrinciple(requestContext, principal);
        return true;
    }
}
