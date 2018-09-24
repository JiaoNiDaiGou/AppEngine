package jiaonidaigou.appengine.api.access.db.core;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.Parser;
import jiaonidaigou.appengine.common.model.BiTransform;
import jiaonidaigou.appengine.common.model.JsonBytesBiTransform;
import jiaonidaigou.appengine.common.model.ProtoBytesBiTransform;
import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class DbClientBuilder<T> {
    private DatastoreService datastoreService;
    private DatastoreEntityFactory<T> entityFactory;
    private boolean useInMemoryCache = false;
    private CacheBuilder<String, T> inMemoryCacheBuilder;
    private boolean useMemcache = false;
    private String memcacheNamespace;
    private BiTransform<T, byte[]> memcacheTransform;
    private MemcacheService memcacheService;

    public DbClientBuilder<T> entityFactory(final DatastoreEntityFactory<T> entityFactory) {
        this.entityFactory = entityFactory;
        return this;
    }

    public DbClientBuilder<T> datastoreService(final DatastoreService datastoreService) {
        this.datastoreService = datastoreService;
        return this;
    }

    public DbClientBuilder<T> memcacheService(final MemcacheService memcacheService) {
        this.memcacheService = memcacheService;
        return this;
    }

    public DbClientBuilder<T> useInMemoryCache() {
        useInMemoryCache = true;
        return this;
    }

    public DbClientBuilder<T> useInMemoryCache(final CacheBuilder<String, T> cacheBuilder) {
        useInMemoryCache = true;
        this.inMemoryCacheBuilder = cacheBuilder;
        return this;
    }

    public DbClientBuilder<T> useMemcache(final String memcacheNamespace,
                                          final BiTransform<T, byte[]> memcacheTransform) {
        this.memcacheNamespace = memcacheNamespace;
        this.memcacheTransform = memcacheTransform;
        this.useMemcache = true;
        return this;
    }

    public DbClientBuilder<T> useMemcacheJsonTransform(final String memcacheNamespace, final Class<T> type) {
        return useMemcache(memcacheNamespace, new JsonBytesBiTransform<>(type));
    }

    @SuppressWarnings("unchecked")
    public DbClientBuilder<T> useMemcacheProtoTransform(final String memcacheNamespace, final Parser<T> parser) {
        return useMemcache(memcacheNamespace, new ProtoBytesBiTransform(parser));
    }

    public DbClient<T> build() {
        checkNotNull(datastoreService);
        checkNotNull(entityFactory);

        DbClient.IdGetter<T> idGetter = t -> entityFactory.getId(t);

        DbClient<T> toReturn = new DatastoreDbClient<>(datastoreService, entityFactory);
        if (useMemcache) {
            checkArgument(StringUtils.isNotBlank(memcacheNamespace));
            checkNotNull(memcacheTransform);
            checkNotNull(memcacheService);
            toReturn = new MemcacheDbClient<>(memcacheNamespace, memcacheService, toReturn, idGetter, memcacheTransform);
        }

        if (useInMemoryCache) {
            if (inMemoryCacheBuilder != null) {
                toReturn = new InMemoryCacheDbClient<>(toReturn, idGetter, inMemoryCacheBuilder);
            } else {
                toReturn = new InMemoryCacheDbClient<>(toReturn, idGetter);
            }
        }
        return toReturn;
    }
}
