package jiaoni.songfan.service.appengine.impls;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaoni.common.appengine.access.db.BaseDbClient;
import jiaoni.common.appengine.access.db.BaseEntityFactory;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DbClientBuilder;
import jiaoni.common.appengine.guice.ENV;
import jiaoni.common.model.Env;
import jiaoni.common.utils.EncryptUtils;
import jiaoni.songfan.service.appengine.AppEnvs;
import jiaoni.songfan.wiremodel.entity.Customer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static jiaoni.common.utils.Preconditions2.checkNotBlank;

@Singleton
public class CustomerDbClient extends BaseDbClient<Customer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerDbClient.class);

    private static final String TABLE_NAME = "Customer";

    private static final String FIELD_PHONE = "phone";

    @Inject
    public CustomerDbClient(@ENV final Env env,
                            final DatastoreService datastoreService) {
        super(new DbClientBuilder<Customer>()
                .datastoreService(datastoreService)
                .entityFactory(new EntityFactory(env))
                .build());
    }

    public static String computeKey(final Customer customer) {
        String rawKey;
        if (customer.hasPhone()) {
            checkNotBlank(customer.getPhone().getCountryCode());
            checkNotBlank(customer.getPhone().getPhone());
            rawKey = String.join("|", "p", customer.getPhone().getCountryCode(), customer.getPhone().getPhone());
        } else if (StringUtils.isNotBlank(customer.getEmail())) {
            rawKey = String.join("|", "e", customer.getEmail());
        } else {
            throw new IllegalArgumentException("cannot compute key for " + customer);
        }
        String key = EncryptUtils.base64Encode(rawKey);
        LOGGER.info("Compute key: from {} to {}", rawKey, key);
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
        protected String getServiceName() {
            return AppEnvs.getServiceName();
        }

        @Override
        protected String getTableName() {
            return TABLE_NAME;
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
                    .indexedString(FIELD_PHONE, obj.getPhone().getPhone())
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
}
