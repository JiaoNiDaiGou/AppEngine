package jiaoni.songfan.service.appengine.utils;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import jiaoni.common.appengine.registry.Registry;
import jiaoni.songfan.service.appengine.AppEnvs;

public class RegistryFactory {
    private static class LazyHolder {
        private static final Registry INSTANCE = new Registry(DatastoreServiceFactory.getDatastoreService(), AppEnvs.getEnv());
    }

    public static Registry get() {
        return LazyHolder.INSTANCE;
    }
}
