package jiaonidaigou.appengine.tools.remote;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;
import jiaonidaigou.appengine.common.utils.Environments;

import java.io.IOException;

/**
 * Initialize resources for calling Appengine remote API.
 * Remote API doc:
 * https://cloud.google.com/appengine/docs/standard/java/tools/remoteapi
 * <p>
 * Prerequisites:
 * 1. Login with Google
 * <pre>
 *     gcloud auth application-default login
 * </pre>
 * It will save your credentials locally to env GOOGLE_APPLICATION_CREDENTIALS, so that
 * GoogleCredentials.getApplicationDefault() can read it.
 * <p>
 * Usage:
 * <pre>
 *     try (RemoteApi remoteApi = RemoteApi.login()) {
 *          // Accessing Datastore service
 *          DatastoreService dbService = remoteApi.getDatastoreService();
 *          ...
 *     }
 * </pre>
 */
public class RemoteApi implements AutoCloseable {
    private final RemoteApiInstaller installer;

    private RemoteApi(final String hostname) {
        RemoteApiOptions options = new RemoteApiOptions()
                .server(hostname, 443).useApplicationDefaultCredential();
        installer = new RemoteApiInstaller();
        try {
            installer.install(options);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static RemoteApi login() {
        return new RemoteApi(Environments.GAE_HOSTNAME);
    }

    public DatastoreService getDatastoreService() {
        return DatastoreServiceFactory.getDatastoreService();
    }

    public AppIdentityService getAppIdentityService() {
        return AppIdentityServiceFactory.getAppIdentityService();
    }

    @Override
    public void close() throws Exception {
        installer.uninstall();
    }
}
