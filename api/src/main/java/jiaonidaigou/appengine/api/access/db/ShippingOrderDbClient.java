package jiaonidaigou.appengine.api.access.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.common.annotations.VisibleForTesting;
import jiaonidaigou.appengine.api.access.db.core.BaseEntityFactory;
import jiaonidaigou.appengine.api.access.db.core.DatastoreDbClient;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityBuilder;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityExtractor;
import jiaonidaigou.appengine.api.access.db.core.DbQuery;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

import static jiaonidaigou.appengine.api.utils.AppEnvironments.ENV;
import static jiaonidaigou.appengine.common.utils.Environments.NAMESPACE_JIAONIDAIGOU;

@Singleton
public class ShippingOrderDbClient extends DatastoreDbClient<ShippingOrder> {
    private static final String TABLE_NAME = "ShippingOrder";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_CREATION_TIME = "creation_time";
    private static final String FIELD_CUSTOMER_ID = "customer_id";
    private static final String FIELD_CUSTOMER_PHONE = "customer_phone";
    private static final String FIELD_CUSTOMER_NAME = "customer_name";
    private static final String FIELD_TEDDY_ORDER_ID = "teddy_order_id";
    private static final String FIELD_STATUS = "status";

    @Inject
    public ShippingOrderDbClient(final DatastoreService datastoreService) {
        this(datastoreService, ENV);
    }

    @VisibleForTesting
    public ShippingOrderDbClient(final DatastoreService datastoreService, final Env env) {
        super(datastoreService, new EntityFactory(NAMESPACE_JIAONIDAIGOU, env, TABLE_NAME));
    }

    public List<ShippingOrder> queryByTeddyOrderIdRange(final long minTeddyIdInclusive,
                                                        final long maxTeddyIdInclusive) {
        DbQuery query = DbQuery.and(
                DbQuery.ge(FIELD_TEDDY_ORDER_ID, String.valueOf(minTeddyIdInclusive)),
                DbQuery.le(FIELD_TEDDY_ORDER_ID, String.valueOf(maxTeddyIdInclusive))
        );
        return this.queryInStream(query)
                .sorted((a, b) -> Long.compare(b.getCreationTime(), a.getCreationTime()))
                .collect(Collectors.toList());
    }

    public List<ShippingOrder> queryNonDeliveredOrders() {
        DbQuery query = DbQuery.notEq(FIELD_STATUS, ShippingOrder.Status.DELIVERED.name());
        return this.queryInStream(query)
                .sorted((a, b) -> Long.compare(b.getCreationTime(), a.getCreationTime()))
                .collect(Collectors.toList());
    }

    private static class EntityFactory extends BaseEntityFactory<ShippingOrder> {

        protected EntityFactory(String serviceName, Env env, String tableName) {
            super(serviceName, env, tableName);
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
            System.out.println("customerId:" + obj.getReceiver().getId());
            return partialBuilder
                    .indexedLong(FIELD_CREATION_TIME, obj.getCreationTime())
                    .indexedString(FIELD_CUSTOMER_ID, obj.getReceiver().getId())
                    .indexedString(FIELD_CUSTOMER_PHONE, obj.getReceiver().getPhone().getPhone())
                    .indexedString(FIELD_CUSTOMER_NAME, obj.getReceiver().getName())
                    .indexedString(FIELD_TEDDY_ORDER_ID, obj.getTeddyOrderId())
                    .indexedEnum(FIELD_STATUS, obj.getStatus())
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
    }
}
