package jiaoni.daigou.service.appengine.impls;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.memcache.MemcacheService;
import jiaoni.common.appengine.access.db.BaseDbClient;
import jiaoni.common.appengine.access.db.BaseEntityFactory;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DbClientBuilder;
import jiaoni.common.appengine.guice.ENV;
import jiaoni.common.model.Env;
import jiaoni.common.utils.EncryptUtils;
import jiaoni.common.wiremodel.PhoneNumber;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.wiremodel.entity.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;
import static jiaoni.common.utils.Preconditions2.checkNotBlank;

@Singleton
public class CustomerDbClient extends BaseDbClient<Customer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerDbClient.class);

    private static final String TABLE_NAME = "Customer";

    @Inject
    public CustomerDbClient(@ENV final Env env,
                            final DatastoreService datastoreService,
                            final MemcacheService memcacheService) {
        super(new DbClientBuilder<Customer>()
                .datastoreService(datastoreService)
                .entityFactory(new EntityFactory(env))
                .memcacheService(memcacheService)
                .memcacheProtoTransform(TABLE_NAME, Customer.parser())
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

    public Customer putAndUpdateTimestamp(final Customer customer) {
        return put(customer.toBuilder().setLastUpdatedTime(System.currentTimeMillis()).build());
    }

    private static class EntityFactory extends BaseEntityFactory<Customer> {
        private static final String FIELD_DATA = "data";

        EntityFactory(Env env) {
            super(env);
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
