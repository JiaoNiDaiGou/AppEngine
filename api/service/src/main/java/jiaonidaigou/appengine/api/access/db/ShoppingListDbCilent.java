package jiaonidaigou.appengine.api.access.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.common.annotations.VisibleForTesting;
import jiaonidaigou.appengine.api.access.db.core.BaseEntityFactory;
import jiaonidaigou.appengine.api.access.db.core.DatastoreDbClient;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityBuilder;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityExtractor;
import jiaonidaigou.appengine.api.access.db.core.DbQuery;
import jiaoni.common.model.Env;
import jiaonidaigou.appengine.wiremodel.entity.ShoppingListItem;

import java.util.Arrays;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

import static jiaonidaigou.appengine.api.utils.AppEnvironments.ENV;
import static jiaoni.common.utils.Environments.NAMESPACE_JIAONIDAIGOU;

@Singleton
public class ShoppingListDbCilent extends DatastoreDbClient<ShoppingListItem> {
    private static final String TABLE_NAME = "ShoppingList";
    private static final String FIELD_DATA = "dat";
    private static final String FIELD_OWNER_NAME = "owner_name";
    private static final String FIELD_STATUS = "status";

    @Inject
    public ShoppingListDbCilent(final DatastoreService datastoreService) {
        this(datastoreService, ENV);
    }

    @VisibleForTesting
    public ShoppingListDbCilent(final DatastoreService datastoreService, final Env env) {
        super(datastoreService, new EntityFactory(NAMESPACE_JIAONIDAIGOU, env, TABLE_NAME));
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
        protected EntityFactory(String serviceName, Env env, String tableName) {
            super(serviceName, env, tableName);
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
