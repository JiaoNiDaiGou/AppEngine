package jiaonidaigou.appengine.api.access.gcp;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import jiaonidaigou.appengine.common.model.InternalIOException;
import jiaonidaigou.appengine.common.model.InternalRuntimeException;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.common.utils.Secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;

import static com.google.api.client.util.Preconditions.checkNotNull;

/**
 * See examples:
 * https://developers.google.com/api-client-library/java/apis/
 * https://github.com/google/google-api-java-client-samples
 */
public class GoogleApisClientFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleApisClientFactory.class);
    private static final JacksonFactory JACKSON_FACTORY = JacksonFactory.getDefaultInstance();

    public static Sheets sheets() {
        return sheets(InitStyle.LOCAL);
    }

    public static Sheets sheets(final InitStyle initStyle) {
        checkNotNull(initStyle);
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            HttpRequestInitializer httpRequestInitializer = initializer(initStyle, httpTransport, SheetsScopes.all());
            return new Sheets.Builder(httpTransport, JACKSON_FACTORY, httpRequestInitializer)
                    .setApplicationName(applicationName(Sheets.class))
                    .build();
        } catch (Exception e) {
            throw new InternalRuntimeException(e);
        }
    }

    private static String applicationName(final Class<? extends AbstractGoogleJsonClient> type) {
        return "jiaonidaigou." + type.getSimpleName();
    }

    private static HttpRequestInitializer initializer(final InitStyle initStyle,
                                                      final HttpTransport httpTransport,
                                                      final Collection<String> scopes) {
        switch (initStyle) {
            case LOCAL:
                return initializerLocally(httpTransport, scopes);
            case GAE:
                return initializerInGae(httpTransport, scopes);
            default:
                throw new IllegalStateException();
        }
    }

    private static HttpRequestInitializer initializerInGae(final HttpTransport httpTransport,
                                                           final Collection<String> scopes) {
        checkNotNull(httpTransport);
        checkNotNull(scopes);
        try {
            return GoogleCredential.getApplicationDefault(httpTransport, JACKSON_FACTORY).createScoped(scopes);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    /**
     * Initializes {@link HttpRequestInitializer} create with local credentials.
     */
    private static HttpRequestInitializer initializerLocally(final HttpTransport httpTransport,
                                                             final Collection<String> scopes) {
        checkNotNull(httpTransport);
        checkNotNull(scopes);

        LOGGER.info("scopes {}", scopes);

        try (Reader reader = Secrets.of("gcp.local.credentials.json").getAsReader()) {
            // Load client secrets
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JACKSON_FACTORY, reader);

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow
                    .Builder(httpTransport, JACKSON_FACTORY, clientSecrets, scopes)
                    .setDataStoreFactory(new FileDataStoreFactory(new File(Environments.LOCAL_TEMP_DIR_ENDSLASH + "gapis")))
                    .setAccessType("offline")
                    .build();

            return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
                    .authorize("jiaonidaigou");
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    public enum InitStyle {
        LOCAL, GAE
    }
}
