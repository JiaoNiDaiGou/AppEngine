package jiaonidaigou.appengine.api.access.db.core;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.common.cache.CacheBuilder;

import static com.google.common.base.Preconditions.checkNotNull;

public class DbClientBuilder<T> {
    private DatastoreService datastoreService;
    private DatastoreEntityFactory<T> entityFactory;
    private boolean inMemoryCache = false;
    private CacheBuilder<String, T> inMemoryCacheBuilder;

    public DbClientBuilder<T> entityFactory(final DatastoreEntityFactory<T> entityFactory) {
        this.entityFactory = entityFactory;
        return this;
    }

    public DbClientBuilder<T> datastoreService(final DatastoreService datastoreService) {
        this.datastoreService = datastoreService;
        return this;
    }

    public DbClientBuilder<T> inMemoryCache() {
        inMemoryCache = true;
        return this;
    }

    public DbClientBuilder<T> inMemoryCache(final CacheBuilder<String, T> cacheBuilder) {
        inMemoryCache = true;
        this.inMemoryCacheBuilder = cacheBuilder;
        return this;
    }

    public DbClient<T> build() {
        checkNotNull(datastoreService);
        checkNotNull(entityFactory);

        DbClient<T> toReturn = new DatastoreDbClient<>(datastoreService, entityFactory);
        if (inMemoryCache) {
            if (inMemoryCacheBuilder != null) {
                toReturn = new InMemoryCacheDbClient<>(toReturn, t -> entityFactory.getId(t), inMemoryCacheBuilder);
            } else {
                toReturn = new InMemoryCacheDbClient<>(toReturn, t -> entityFactory.getId(t));
            }
        }
        return toReturn;
    }
}
