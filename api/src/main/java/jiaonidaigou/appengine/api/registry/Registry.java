package jiaonidaigou.appengine.api.registry;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.common.annotations.VisibleForTesting;
import jiaonidaigou.appengine.api.access.db.core.BaseDbClient;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityBuilder;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityExtractor;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityFactory;
import jiaonidaigou.appengine.api.access.db.core.DbClient;
import jiaonidaigou.appengine.api.access.db.core.DbClientBuilder;
import org.apache.commons.lang3.tuple.Pair;

public class Registry extends BaseDbClient<Pair<String, String>> {
    private static final String KIND = "Registry";
    private static final String FIELD_VAL = "val";

    /**
     * Use {@link #instance()}.
     */
    @VisibleForTesting
    public Registry(final DatastoreService service) {
        super(new DbClientBuilder<Pair<String, String>>()
                .datastoreService(service)
                .entityFactory(new EntityFactory())
                .build());
    }

    @VisibleForTesting
    public Registry(final DbClient<Pair<String, String>> dbClient) {
        super(dbClient);
    }

    public static Registry instance() {
        return LazyHolder.instance;
    }

    private static String key(final String serviceName, final String key) {
        return serviceName + "." + key;
    }

    public void setRegistry(final String serviceName, final String key, final String val) {
        put(Pair.of(key(serviceName, key), val));
    }

    public String getRegistry(final String serviceName, final String key) {
        Pair<String, String> pair = getById(key(serviceName, key));
        return pair == null ? null : pair.getRight();
    }

    private static class LazyHolder {
        private static Registry instance = new Registry(DatastoreServiceFactory.getDatastoreService());
    }

    private static class EntityFactory implements DatastoreEntityFactory<Pair<String, String>> {
        @Override
        public KeyType getKeyType() {
            return KeyType.STRING_NAME;
        }

        @Override
        public String getKind() {
            return KIND;
        }

        @Override
        public Pair<String, String> fromEntity(DatastoreEntityExtractor entity) {
            return Pair.of(entity.getKeyStringName(), entity.getAsText(FIELD_VAL));
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, Pair<String, String> obj) {
            return partialBuilder.unindexedText(FIELD_VAL, obj.getRight())
                    .build();
        }

        @Override
        public Pair<String, String> mergeId(Pair<String, String> obj, String id) {
            return Pair.of(id, obj.getRight());
        }

        @Override
        public String getId(Pair<String, String> obj) {
            return obj.getLeft();
        }
    }
}
