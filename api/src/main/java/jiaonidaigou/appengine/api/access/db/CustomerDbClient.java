package jiaonidaigou.appengine.api.access.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.common.annotations.VisibleForTesting;
import jiaonidaigou.appengine.api.access.db.core.BaseDbClient;
import jiaonidaigou.appengine.api.access.db.core.BaseEntityFactory;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityBuilder;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityExtractor;
import jiaonidaigou.appengine.api.access.db.core.DbClientBuilder;
import jiaoni.common.model.Env;
import jiaoni.common.utils.EncryptUtils;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static jiaonidaigou.appengine.api.utils.AppEnvironments.ENV;
import static jiaoni.common.utils.Preconditions2.checkNotBlank;

public class CustomerDbClient extends BaseDbClient<Customer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerDbClient.class);

    public CustomerDbClient(final DatastoreService datastoreService,
                            final MemcacheService memcacheService,
                            final String serviceName) {
        this(datastoreService, memcacheService, serviceName, ENV);
    }

    @VisibleForTesting
    public CustomerDbClient(final DatastoreService datastoreService,
                            final MemcacheService memcacheService,
                            final String serviceName,
                            final Env env) {
        super(new DbClientBuilder<Customer>()
                .datastoreService(datastoreService)
                .entityFactory(new EntityFactory(serviceName, env, "Customer"))
                .memcacheService(memcacheService)
                .memcacheProtoTransform("JNDG.Customer", Customer.parser())
                .memcacheAll()
                .build());
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

    private static class EntityFactory extends BaseEntityFactory<Customer> {
        private static final String FIELD_DATA = "data";

        protected EntityFactory(String serviceName, Env env, String tableName) {
            super(serviceName, env, tableName);
        }

        @Override
        public KeyType getKeyType() {
            return KeyType.STRING_NAME;
        }

        @Override
        public Customer fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, Customer.parser());
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

    public Customer putAndUpdateTimestamp(final Customer customer) {
        return put(customer.toBuilder().setLastUpdatedTime(System.currentTimeMillis()).build());
    }
}
