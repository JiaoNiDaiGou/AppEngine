package jiaoni.daigou.service.appengine.utils;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import jiaoni.common.appengine.registry.Registry;
import jiaoni.daigou.service.appengine.AppEnvs;

public class RegistryFactory {
    public interface Keys {
        String WxSyncTaskRunner_ALLOW_NEXT_TASK = "WxSyncTaskRunner.ALLOW_NEXT_TASK";
        String WxSyncTaskRunner_RUN_FOREVER = "WxSyncTaskRunner.RUN_FOREVER";
    }

    private static class LazyHolder {
        private static final Registry INSTANCE = new Registry(DatastoreServiceFactory.getDatastoreService(), AppEnvs.getEnv());
    }

    public static Registry get() {
        return LazyHolder.INSTANCE;
    }
}
