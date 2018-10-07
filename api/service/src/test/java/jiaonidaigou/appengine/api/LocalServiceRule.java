package jiaonidaigou.appengine.api;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.Streams;
import org.junit.rules.ExternalResource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LocalServiceRule extends ExternalResource {
    private boolean useDatastore;
    private LocalServiceTestHelper helper;
    private LocalDatastoreServiceTestConfig datastoreServiceTestConfig;
    private DatastoreService datastoreService;

    public static Builder builder() {
        return new Builder();
    }

    private LocalServiceRule(final Builder builder) {
        List<LocalServiceTestConfig> configs = new ArrayList<>();
        this.useDatastore = builder.useDatastore;
        if (useDatastore) {
            datastoreServiceTestConfig = new LocalDatastoreServiceTestConfig();
            configs.add(datastoreServiceTestConfig);
        }
        helper = new LocalServiceTestHelper(configs.toArray(new LocalServiceTestConfig[]{ }));
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

    public static final class Builder {
        private boolean useDatastore;

        private Builder() {
        }

        public Builder useDatastore() {
            useDatastore = true;
            return this;
        }

        public LocalServiceRule build() {
            return new LocalServiceRule(this);
        }
    }
}
