package jiaoni.common.appengine.access;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.rules.ExternalResource;

import java.util.ArrayList;
import java.util.List;

public class LocalServiceRule extends ExternalResource {
    private LocalServiceTestHelper helper;
    private DatastoreService datastoreService;
    private MemcacheService memcacheService;

    private LocalServiceRule(final Builder builder) {
        List<LocalServiceTestConfig> configs = new ArrayList<>();
        if (builder.useDatastore) {
            LocalDatastoreServiceTestConfig datastoreServiceTestConfig = new LocalDatastoreServiceTestConfig();
            configs.add(datastoreServiceTestConfig);
        }
        if (builder.useMemcache) {
            LocalMemcacheServiceTestConfig memcacheServiceTestConfig = new LocalMemcacheServiceTestConfig()
                    .setMaxSize(1, LocalMemcacheServiceTestConfig.SizeUnit.MB);
            configs.add(memcacheServiceTestConfig);
        }
        helper = new LocalServiceTestHelper(configs.toArray(new LocalServiceTestConfig[]{ }));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void before() {
        helper.setUp();
    }

    @Override
    protected void after() {
        helper.tearDown();
    }

    public DatastoreService getDatastoreService() {
        if (datastoreService == null) {
            datastoreService = DatastoreServiceFactory.getDatastoreService();
        }
        return datastoreService;
    }

    public MemcacheService memcacheService() {
        if (memcacheService == null) {
            memcacheService = MemcacheServiceFactory.getMemcacheService();
        }
        return memcacheService;
    }

    public static final class Builder {
        private boolean useDatastore;
        private boolean useMemcache;

        private Builder() {
        }

        public Builder useDatastore() {
            useDatastore = true;
            return this;
        }

        public Builder useMemcache() {
            useMemcache = true;
            return this;
        }

        public LocalServiceRule build() {
            return new LocalServiceRule(this);
        }
    }
}
