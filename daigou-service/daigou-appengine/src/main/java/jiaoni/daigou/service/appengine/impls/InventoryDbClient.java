package jiaoni.daigou.service.appengine.impls;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.cloud.sql.jdbc.internal.Charsets;
import jiaoni.common.appengine.access.db.BaseDbClient;
import jiaoni.common.appengine.access.db.BaseEntityFactory;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DbClientBuilder;
import jiaoni.common.appengine.guice.ENV;
import jiaoni.common.model.BiTransform;
import jiaoni.common.model.Env;
import jiaoni.daigou.service.appengine.AppEnvs;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class InventoryDbClient extends BaseDbClient<Triple<String, Integer, Long>> {
    private static final String TABLE_NAME = "Inventory";

    private static final String FIELD_PRODUCT_ID = "productId";
    private static final String FIELD_QUANTITY = "quantity";
    private static final String FIELD_LAST_UPDATE_TIME = "lastUpdateTime";

    @Inject
    public InventoryDbClient(@ENV final Env env,
                             final DatastoreService datastoreService,
                             final MemcacheService memcacheService) {
        super(new DbClientBuilder<Triple<String, Integer, Long>>()
                .datastoreService(datastoreService)
                .entityFactory(new EntityFactory(env))
                .memcache(TABLE_NAME, new MemcacheTransform())
                .memcacheService(memcacheService)
                .memcacheAll()
                .build());
    }

    private static class MemcacheTransform implements BiTransform<Triple<String, Integer, Long>, byte[]> {

        @Override
        public byte[] to(Triple<String, Integer, Long> from) {
            if (from == null) {
                return null;
            }
            return String.join("|",
                    from.getLeft(),
                    String.valueOf(from.getMiddle()),
                    String.valueOf(from.getRight()))
                    .getBytes(Charsets.UTF_8);
        }

        @Override
        public Triple<String, Integer, Long> from(byte[] to) {
            if (to == null || to.length == 0) {
                return Triple.of(null, null, null);
            }
            String[] parts = StringUtils.split(new String(to), "|");
            return Triple.of(parts[0], Integer.parseInt(parts[1]), Long.parseLong(parts[2]));
        }
    }

    private static class EntityFactory extends BaseEntityFactory<Triple<String, Integer, Long>> {
        protected EntityFactory(Env env) {
            super(env);
        }

        @Override
        protected String getServiceName() {
            return AppEnvs.getServiceName();
        }

        @Override
        protected String getTableName() {
            return TABLE_NAME;
        }

        @Override
        public KeyType getKeyType() {
            return null;
        }

        @Override
        public Triple<String, Integer, Long> fromEntity(DatastoreEntityExtractor entity) {
            return Triple.of(
                    entity.getAsString(FIELD_PRODUCT_ID),
                    entity.getAsInteger(FIELD_QUANTITY),
                    entity.getAsLong(FIELD_LAST_UPDATE_TIME));
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, Triple<String, Integer, Long> obj) {
            return partialBuilder
                    .indexedString(FIELD_PRODUCT_ID, obj.getLeft())
                    .unindexedInteger(FIELD_QUANTITY, obj.getMiddle())
                    .unindexedLong(FIELD_LAST_UPDATE_TIME, obj.getRight())
                    .build();
        }

        @Override
        public Triple<String, Integer, Long> mergeId(Triple<String, Integer, Long> obj, String id) {
            return Triple.of(id, obj.getMiddle(), obj.getRight());
        }

        @Override
        public String getId(Triple<String, Integer, Long> obj) {
            return obj.getLeft();
        }
    }
}
