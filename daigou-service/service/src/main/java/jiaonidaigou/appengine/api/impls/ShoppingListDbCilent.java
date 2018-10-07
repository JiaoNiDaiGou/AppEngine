package jiaonidaigou.appengine.api.impls;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.common.annotations.VisibleForTesting;
import jiaoni.common.appengine.access.db.BaseEntityFactory;
import jiaoni.common.appengine.access.db.DatastoreDbClient;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DbQuery;
import jiaoni.common.model.Env;
import jiaoni.daigou.wiremodel.entity.ShoppingListItem;
import jiaonidaigou.appengine.api.AppEnvs;

import java.util.Arrays;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShoppingListDbCilent extends DatastoreDbClient<ShoppingListItem> {
    private static final String TABLE_NAME = "ShoppingList";
    private static final String FIELD_DATA = "dat";
    private static final String FIELD_OWNER_NAME = "owner_name";
    private static final String FIELD_STATUS = "status";

    @Inject
    public ShoppingListDbCilent(final DatastoreService datastoreService) {
        this(datastoreService, AppEnvs.getEnv());
    }

    @VisibleForTesting
    public ShoppingListDbCilent(final DatastoreService datastoreService, final Env env) {
        super(datastoreService, new EntityFactory(env, TABLE_NAME));
    }

    public Stream<ShoppingListItem> queryByStatus(final ShoppingListItem.Status status) {
        DbQuery dbQuery = DbQuery.eq(FIELD_STATUS, status.name());
        return this.queryInStream(dbQuery);
    }

    public Stream<ShoppingListItem> queryActive() {
        DbQuery dbQuery = DbQuery.or(Arrays.asList(
                DbQuery.eq(FIELD_STATUS, ShoppingListItem.Status.INIT.name()),
                DbQuery.eq(FIELD_STATUS, ShoppingListItem.Status.OWNERSHIP_ASSIGNED.name()),
                DbQuery.eq(FIELD_STATUS, ShoppingListItem.Status.PURCHASED.name())
        ));
        return this.queryInStream(dbQuery);
    }

    private static class EntityFactory extends BaseEntityFactory<ShoppingListItem> {
        protected EntityFactory(Env env, String tableName) {
            super(AppEnvs.getServiceName(), env, tableName);
        }

        @Override
        public KeyType getKeyType() {
            return KeyType.LONG_ID;
        }

        @Override
        public ShoppingListItem fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, ShoppingListItem.parser());
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, ShoppingListItem obj) {
            return partialBuilder
                    .indexedString(FIELD_STATUS, obj.getStatus().name())
                    .indexedString(FIELD_OWNER_NAME, obj.getOwnerName())
                    .unindexedProto(FIELD_DATA, obj)
                    .unindexedLastUpdatedTimestampAsNow()
                    .build();
        }

        @Override
        public ShoppingListItem mergeId(ShoppingListItem obj, String id) {
            return obj.toBuilder().setId(id).build();
        }

        @Override
        public String getId(ShoppingListItem obj) {
            return obj.getId();
        }
    }
}
