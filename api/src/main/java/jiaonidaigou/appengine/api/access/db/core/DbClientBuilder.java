package jiaonidaigou.appengine.api.access.db.core;

import com.google.appengine.api.datastore.DatastoreService;

import static com.google.common.base.Preconditions.checkNotNull;

public class DbClientBuilder<T> {
    private DatastoreService datastoreService;
    private DatastoreEntityFactory<T> entityFactory;

    public DbClientBuilder<T> entityFactory(final DatastoreEntityFactory<T> entityFactory) {
        this.entityFactory = entityFactory;
        return this;
    }

    public DbClientBuilder<T> datastoreService(final DatastoreService datastoreService) {
        this.datastoreService = datastoreService;
        return this;
    }

    public DbClient<T> build() {
        checkNotNull(datastoreService);
        checkNotNull(entityFactory);
        return new DatastoreDbClient<>(datastoreService, entityFactory);
    }
}
