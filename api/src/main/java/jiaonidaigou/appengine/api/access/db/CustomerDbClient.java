package jiaonidaigou.appengine.api.access.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaonidaigou.appengine.api.access.db.core.BaseDbClient;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityBuilder;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityExtractor;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityFactory;
import jiaonidaigou.appengine.api.access.db.core.DbClientBuilder;
import jiaonidaigou.appengine.common.utils.EncryptUtils;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static jiaonidaigou.appengine.api.utils.AppEnvironments.ENV;
import static jiaonidaigou.appengine.common.utils.Preconditions2.checkNotBlank;

public class CustomerDbClient extends BaseDbClient<Customer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerDbClient.class);

    public CustomerDbClient(final DatastoreService service, final String serviceName) {
        super(new DbClientBuilder<Customer>()
                .datastoreService(service)
                .entityFactory(new EntityFactory(serviceName + "." + ENV + ".Customer"))
                .inMemoryCache()
                .build());
    }

    private static class EntityFactory implements DatastoreEntityFactory<Customer> {
        private static final String FIELD_DATA = "data";

        private final String kind;

        EntityFactory(final String kind) {
            this.kind = kind;
        }

        @Override
        public KeyType getKeyType() {
            return KeyType.STRING_NAME;
        }

        @Override
        public String getKind() {
            return kind;
        }

        @Override
        public Customer fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, Customer.parser())
                    .toBuilder()
                    .setId(entity.getKeyStringName())
                    .build();
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, Customer obj) {
            return partialBuilder.unindexedProto(FIELD_DATA, obj)
                    .unindexedLastUpdatedTimestampAsNow()
                    .build();
        }

        @Override
        public String getId(Customer obj) {
            return obj.getId();
        }

        @Override
        public Customer mergeId(Customer obj, String id) {
            return obj.toBuilder().setId(id).build();
        }
    }

    public static String computeKey(final PhoneNumber phone, final String name) {
        checkNotNull(phone);
        checkNotBlank(phone.getCountryCode());
        checkNotBlank(phone.getPhone());
        checkNotBlank(name);

        String key = EncryptUtils.base64Encode(phone.getCountryCode() + "|" + phone.getPhone() + "|" + name);
        LOGGER.info("Compute key: from {}|{}|{} to {}", phone.getCountryCode(), phone.getPhone(), name, key);
        return key;
    }
}
