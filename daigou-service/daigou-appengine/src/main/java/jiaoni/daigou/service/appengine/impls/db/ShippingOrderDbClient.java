package jiaoni.daigou.service.appengine.impls.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaoni.common.appengine.access.db.BaseEntityFactory;
import jiaoni.common.appengine.access.db.DatastoreDbClient;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DbQuery;
import jiaoni.common.appengine.guice.ENV;
import jiaoni.common.model.Env;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.wiremodel.entity.ShippingOrder;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShippingOrderDbClient extends DatastoreDbClient<ShippingOrder> {
    private static final String TABLE_NAME = "ShippingOrder";
    private static final String FIELD_DATA = "data";
    public static final String FIELD_CREATION_TIME = "creation_time";
    public static final String FIELD_CUSTOMER_ID = "customer_id";
    public static final String FIELD_STATUS_NUM = "status_num";

    @Inject
    public ShippingOrderDbClient(@ENV final Env env, final DatastoreService datastoreService) {
        super(datastoreService, new EntityFactory(env));
    }

    @Deprecated
    public List<ShippingOrder> queryNonDeliveredShippingOrders() {
        DbQuery query = DbQuery.notEq(FIELD_STATUS_NUM, ShippingOrder.Status.DELIVERED_VALUE);
        return this.queryInStream(query)
                .sorted((a, b) -> Long.compare(b.getCreationTime(), a.getCreationTime()))
                .collect(Collectors.toList());
    }

    private static class EntityFactory extends BaseEntityFactory<ShippingOrder> {
        EntityFactory(Env env) {
            super(env);
        }

        @Override
        public KeyType getKeyType() {
            return KeyType.LONG_ID;
        }

        @Override
        public ShippingOrder fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, ShippingOrder.parser());
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, ShippingOrder obj) {
            return partialBuilder
                    .indexedLong(FIELD_CREATION_TIME, obj.getCreationTime())
                    .indexedString(FIELD_CUSTOMER_ID, obj.getReceiver().getId())
                    .indexedInteger(FIELD_STATUS_NUM, obj.getStatusValue())
                    .unindexedProto(FIELD_DATA, obj)
                    .unindexedLastUpdatedTimestampAsNow()
                    .build();
        }

        @Override
        public String getId(ShippingOrder obj) {
            return obj.getId();
        }

        @Override
        public ShippingOrder mergeId(ShippingOrder obj, String id) {
            return obj.toBuilder().setId(id).build();
        }

        @Override
        protected String getServiceName() {
            return AppEnvs.getServiceName();
        }

        @Override
        protected String getTableName() {
            return TABLE_NAME;
        }
    }
}
