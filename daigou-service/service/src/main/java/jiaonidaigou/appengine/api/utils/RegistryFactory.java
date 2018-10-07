package jiaonidaigou.appengine.api.utils;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import jiaoni.common.appengine.registry.Registry;
import jiaonidaigou.appengine.api.AppEnvs;

public class RegistryFactory {
    private static class LazyHolder {
        private static final Registry INSTANCE = new Registry(DatastoreServiceFactory.getDatastoreService(), AppEnvs.getEnv());
    }

    public static Registry get() {
        return LazyHolder.INSTANCE;
    }
}
