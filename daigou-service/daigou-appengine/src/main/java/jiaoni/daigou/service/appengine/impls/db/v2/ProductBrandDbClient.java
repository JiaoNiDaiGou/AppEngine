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
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.v2.entity.ProductBrand;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProductBrandDbClient extends BaseDbClient<ProductBrand> {
    private static final String TABLE_NAME = "ProductBrand";

    @Inject
    public ProductBrandDbClient(@ENV final Env env,
                                final DatastoreService datastoreService,
                                final MemcacheService memcacheService) {
        super(new DbClientBuilder<ProductBrand>()
                .datastore(DbClientBuilder.<ProductBrand>datastoreSettings()
                        .datastoreService(datastoreService)
                        .entityFactory(new EntityFactory(env)))
                .memcache(DbClientBuilder.<ProductBrand>memcacheSettings()
                        .memcacheService(memcacheService)
                        .namespace(TABLE_NAME)
                        .protoTransform(ProductBrand.parser())
                )
                .build()
        );
    }

    private static class EntityFactory extends BaseEntityFactory<ProductBrand> {
        private static final String FIELD_DATA = "data";

        protected EntityFactory(Env env) {
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
        public ProductBrand fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, ProductBrand.parser());
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, ProductBrand obj) {
            return partialBuilder.unindexedProto(FIELD_DATA, obj)
                    .build();
        }

        @Override
        public ProductBrand mergeId(ProductBrand obj, String id) {
            return obj.toBuilder().setId(id).build();
        }

        @Override
        public String getId(ProductBrand obj) {
            return obj.getId();
        }
    }
}
