package jiaonidaigou.appengine.api.access.gcp;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jiaoni.common.utils.Environments;

public class GoogleCloudLibFactory {
    public static Storage storage() {
        return StorageOptions.newBuilder()
                .setProjectId(Environments.GAE_PROJECT_ID)
                .build()
                .getService();
    }
}
