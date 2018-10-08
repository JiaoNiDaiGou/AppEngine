package jiaoni.songfan.service.appengine.impls;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaoni.common.appengine.access.db.BaseDbClient;
import jiaoni.common.appengine.access.db.BaseEntityFactory;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DbClient;
import jiaoni.common.appengine.access.db.DbClientBuilder;
import jiaoni.common.appengine.guice.ENV;
import jiaoni.common.model.Env;
import jiaoni.songfan.service.appengine.AppEnvs;
import jiaoni.songfan.wiremodel.entity.Order;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OrderDbClient extends BaseDbClient<Order> {
    private static final String TABLE_NAME = "Order";

    private static final String FIELD_CUSTOMER_ID = "customer_id";
    private static final String FIELD_CUSTOMER_PHONE = "customer_phone";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_TAGS = "tags";

    @Inject
    public OrderDbClient(@ENV final Env env,
                         final DatastoreService datastoreService) {
        super(new DbClientBuilder<Order>()
                .datastoreService(datastoreService)
                .entityFactory(new EntityFactory(env))
                .build());
    }

    private static class EntityFactory extends BaseEntityFactory<Order> {
        EntityFactory(Env env) {
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
            return KeyType.LONG_ID;
        }

        @Override
        public Order fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, Order.parser());
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, Order obj) {
            return partialBuilder
                    .indexedString(FIELD_CUSTOMER_ID, obj.getCustomer().getId())
                    .indexedString(FIELD_CUSTOMER_PHONE, obj.getCustomer().getPhone().getPhone())
                    .indexedEnum(FIELD_STATUS, obj.getStatus())
                    .indexedStringList(FIELD_TAGS, obj.getTagsList())
                    .unindexedProto(FIELD_DATA, obj)
                    .unindexedLastUpdatedTimestampAsNow()
                    .build();
        }

        @Override
        public Order mergeId(Order obj, String id) {
            return obj.toBuilder().setId(id).build();
        }

        @Override
        public String getId(Order obj) {
            return obj.getId();
        }
    }

    public OrderDbClient(DbClient client) {
        super(client);
    }

}
