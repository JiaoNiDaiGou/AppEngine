package jiaonidaigou.appengine.api.access.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaonidaigou.appengine.api.access.db.core.DatastoreDbClient;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityBuilder;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityExtractor;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityFactory;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;

import javax.inject.Inject;
import javax.inject.Singleton;

import static jiaonidaigou.appengine.api.utils.AppEnvironments.ENV;
import static jiaonidaigou.appengine.common.utils.Environments.SERVICE_NAME_JIAONIDAIGOU;

@Singleton
public class ShippingOrderDbClient extends DatastoreDbClient<ShippingOrder> {
    private static final String KIND = SERVICE_NAME_JIAONIDAIGOU + "." + ENV + ".ShippingOrder";
    private static final String FIELD_DATA = "data";

    @Inject
    public ShippingOrderDbClient(final DatastoreService datastoreService) {
        super(datastoreService, new EntityFactory());
    }

    private static class EntityFactory implements DatastoreEntityFactory<ShippingOrder> {

        @Override
        public KeyType getKeyType() {
            return KeyType.LONG_ID;
        }

        @Override
        public String getKind() {
            return KIND;
        }

        @Override
        public ShippingOrder fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, ShippingOrder.parser());
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, ShippingOrder obj) {
            return partialBuilder
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
