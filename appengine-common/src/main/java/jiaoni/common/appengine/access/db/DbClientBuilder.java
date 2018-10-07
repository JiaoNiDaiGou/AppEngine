package jiaoni.common.appengine.access.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.protobuf.Parser;
import jiaoni.common.model.BiTransform;
import jiaoni.common.model.JsonBytesBiTransform;
import jiaoni.common.model.ProtoBytesBiTransform;
import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class DbClientBuilder<T> {
    private DatastoreService datastoreService;
    private DatastoreEntityFactory<T> entityFactory;

    // Memcache settings
    private boolean useMemcache = false;
    private String memcacheNamespace;
    private BiTransform<T, byte[]> memcacheTransform;
    private MemcacheService memcacheService;
    private boolean memcacheAll = false;

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

    public DbClientBuilder<T> useMemcache(final String memcacheNamespace,
                                          final BiTransform<T, byte[]> memcacheTransform) {
        this.memcacheNamespace = memcacheNamespace;
        this.memcacheTransform = memcacheTransform;
        this.useMemcache = true;
        return this;
    }

    public DbClientBuilder<T> memcacheJsonTransform(final String memcacheNamespace, final Class<T> type) {
        return useMemcache(memcacheNamespace, new JsonBytesBiTransform<>(type));
    }

    @SuppressWarnings("unchecked")
    public DbClientBuilder<T> memcacheProtoTransform(final String memcacheNamespace, final Parser<T> parser) {
        return useMemcache(memcacheNamespace, new ProtoBytesBiTransform(parser));
    }

    /**
     * Use memcache to cache all items (scan())
     */
    public DbClientBuilder<T> memcacheAll() {
        this.memcacheAll = true;
        return this;
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
            toReturn = new MemcacheDbClient<>(memcacheNamespace, memcacheService, toReturn, idGetter, memcacheTransform, memcacheAll);
        }

        return toReturn;
    }
}
