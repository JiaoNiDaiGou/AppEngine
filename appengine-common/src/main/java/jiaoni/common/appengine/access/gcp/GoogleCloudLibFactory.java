package jiaoni.common.appengine.access.gcp;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jiaoni.common.utils.Envs;

public class GoogleCloudLibFactory {
    public static Storage storage() {
        return StorageOptions.newBuilder()
                .setProjectId(Envs.GAE_PROJECT_ID)
                .build()
                .getService();
    }
}
