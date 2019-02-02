package jiaoni.daigou.service.appengine.impls.db.v2;

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
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.v2.entity.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

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
                .datastore(DbClientBuilder.<Customer>datastoreSettings()
                        .datastoreService(datastoreService)
                        .entityFactory(new EntityFactory(env)))
                .memcache(DbClientBuilder.<Customer>memcacheSettings()
                        .memcacheService(memcacheService)
                        .namespace(TABLE_NAME)
                        .protoTransform(Customer.parser())
                )
                .build()
        );
    }

    public static String computeKey(final String name, final String phone) {
        checkNotBlank(name);
        checkNotBlank(phone);
        String key = EncryptUtils.base64Encode(name + "|" + phone);
        LOGGER.info("Compute key: from {}|{} to {}", name, phone, key);
        return key;
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
