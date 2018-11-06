package jiaoni.common.appengine.registry;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaoni.common.appengine.access.db.BaseDbClient;
import jiaoni.common.appengine.access.db.BaseEntityFactory;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DbClientBuilder;
import jiaoni.common.model.Env;
import jiaoni.common.utils.Envs;
import org.apache.commons.lang3.tuple.Pair;

public class Registry extends BaseDbClient<Pair<String, String>> {
    private static final String TABLE_NAME = "Registry";
    private static final String FIELD_VAL = "val";

    public Registry(final DatastoreService service, final Env env) {
        super(new DbClientBuilder<Pair<String, String>>()
                .datastoreService(service)
                .entityFactory(new EntityFactory(env))
                .build());
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

    public int getRegistryAsInt(final String serviceName, final String key, final int defaultVal) {
        String val = getRegistry(serviceName, key);
        return val == null ? defaultVal : Integer.parseInt(val);
    }

    public boolean getRegistryAsBoolean(final String serviceName, final String key, final boolean defaultVal) {
        String val = getRegistry(serviceName, key);
        return val == null ? defaultVal : Boolean.parseBoolean(val);
    }

    public void deleteRegistry(final String serviceName, final String key) {
        delete(key(serviceName, key));
    }

    private static class EntityFactory extends BaseEntityFactory<Pair<String, String>> {
        protected EntityFactory(Env env) {
            super(env);
        }

        @Override
        public KeyType getKeyType() {
            return KeyType.STRING_NAME;
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

        @Override
        protected String getServiceName() {
            return Envs.NAMESPACE_SYS;
        }

        @Override
        protected String getTableName() {
            return TABLE_NAME;
        }
    }
}
