package jiaonidaigou.appengine.api.utils;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
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

public class GApisFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(GApisFactory.class);
    private static final JacksonFactory JACKSON_FACTORY = JacksonFactory.getDefaultInstance();

    public static Storage storage() {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            HttpRequestInitializer httpRequestInitializer = initializeLocally(httpTransport, StorageScopes.all());
            return new Storage.Builder(httpTransport, JACKSON_FACTORY, httpRequestInitializer)
                    .setApplicationName(applicationName(Storage.class))
                    .build();
        } catch (Exception e) {
            throw new InternalRuntimeException(e);
        }
    }

    public static Sheets sheets() {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            HttpRequestInitializer httpRequestInitializer = initializeLocally(httpTransport, SheetsScopes.all());
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

    /**
     * Initializes {@link HttpRequestInitializer} create with local credentials.
     */
    private static HttpRequestInitializer initializeLocally(final HttpTransport httpTransport,
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
                    .setDataStoreFactory(new FileDataStoreFactory(new File(Environments.LOCAL_TEMP_DIR_ENDSLASH)))
                    .setAccessType("offline")
                    .build();

            return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
                    .authorize("jiaonidaigou");
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }
}
