package jiaoni.common.appengine.access.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.protobuf.Parser;
import jiaoni.common.model.BiTransform;
import jiaoni.common.model.JsonBytesBiTransform;
import jiaoni.common.model.ProtoBytesBiTransform;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static jiaoni.common.utils.Preconditions2.checkNotBlank;

public class DbClientBuilder<T> {
    private static final Set<String> KNOWN_MEMCACHE_NAMESPACES = new HashSet<>();

    private DatastoreSettings<T> datastoreSettings;
    private MemcacheSettings<T> memcacheSettings;

    public DbClientBuilder<T> datastore(final DatastoreSettings<T> datastoreSettings) {
        this.datastoreSettings = datastoreSettings;
        return this;
    }

    public DbClientBuilder<T> memcache(final MemcacheSettings<T> memcacheSettings) {
        this.memcacheSettings = memcacheSettings;
        return this;
    }

    public static <T> DatastoreSettings<T> datastoreSettings() {
        return new DatastoreSettings<>();
    }

    public static <T> MemcacheSettings<T> memcacheSettings() {
        return new MemcacheSettings<>();
    }

    public DbClient<T> build() {
        checkNotNull(datastoreSettings);
        checkNotNull(datastoreSettings.datastoreService);
        checkNotNull(datastoreSettings.entityFactory);

        DbClient<T> toReturn = new DatastoreDbClient<>(datastoreSettings.datastoreService, datastoreSettings.entityFactory);

        if (memcacheSettings != null) {
            checkNotBlank(memcacheSettings.namespace);
//            checkState(KNOWN_MEMCACHE_NAMESPACES.add(memcacheSettings.namespace), "Duplicated Memcache namespace " + memcacheSettings.namespace);
            checkNotNull(memcacheSettings.memcacheService);
            checkNotNull(memcacheSettings.transform);

            DbClient.IdGetter<T> idGetter = t -> datastoreSettings.entityFactory.getId(t);

            toReturn = new MemcacheDbClient<>(
                    memcacheSettings.namespace,
                    memcacheSettings.memcacheService,
                    toReturn,
                    idGetter,
                    memcacheSettings.transform,
                    memcacheSettings.cacheAllData
            );
        }

        return toReturn;
    }

    /**
     * Datastore settings.
     *
     * @param <T> Type of object.
     */
    public static class DatastoreSettings<T> {
        private DatastoreService datastoreService;
        private DatastoreEntityFactory<T> entityFactory;

        private DatastoreSettings() {
        }

        public DatastoreSettings<T> entityFactory(final DatastoreEntityFactory<T> entityFactory) {
            this.entityFactory = entityFactory;
            return this;
        }

        public DatastoreSettings<T> datastoreService(final DatastoreService datastoreService) {
            this.datastoreService = datastoreService;
            return this;
        }
    }

    /**
     * Memcache settings.
     *
     * @param <T> Type of object.
     */
    public static class MemcacheSettings<T> {
        private String namespace;

        /**
         * If true, will cache all data using bucket.
         * It is suitable for caching small amount of data.
         */
        private boolean cacheAllData;

        /**
         * Transform object to bytes, to be stored in memcache.
         */
        private BiTransform<T, byte[]> transform;

        private MemcacheService memcacheService;

        private MemcacheSettings() {
        }

        public MemcacheSettings<T> memcacheService(final MemcacheService memcacheService) {
            this.memcacheService = memcacheService;
            return this;
        }

        public MemcacheSettings<T> jsonTransform(final Class<T> type) {
            return transform(new JsonBytesBiTransform<>(type));
        }

        @SuppressWarnings("unchecked")
        public MemcacheSettings<T> protoTransform(final Parser<T> parser) {
            return transform(new ProtoBytesBiTransform(parser));
        }

        public MemcacheSettings<T> transform(final BiTransform<T, byte[]> transform) {
            this.transform = transform;
            return this;
        }

        public MemcacheSettings<T> namespace(final String namespace) {
            this.namespace = namespace;
            return this;
        }
    }
}
