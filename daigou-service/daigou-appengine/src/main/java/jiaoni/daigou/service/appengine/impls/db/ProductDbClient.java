package jiaoni.daigou.service.appengine.impls.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.memcache.MemcacheService;
import jiaoni.common.appengine.access.db.BaseDbClient;
import jiaoni.common.appengine.access.db.BaseEntityFactory;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DbClientBuilder;
import jiaoni.common.appengine.access.db.DbQuery;
import jiaoni.common.appengine.guice.ENV;
import jiaoni.common.model.Env;
import jiaoni.common.utils.EncryptUtils;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.daigou.wiremodel.entity.ProductCategory;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class ProductDbClient extends BaseDbClient<Product> {
    private static final String TABLE_NAME = "Product";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_BRAND = "brand";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_HASH = "hash";

    @Inject
    public ProductDbClient(@ENV final Env env,
                           final DatastoreService datastoreService,
                           final MemcacheService memcacheService) {
        super(new DbClientBuilder<Product>()
                .datastore(DbClientBuilder.<Product>datastoreSettings()
                        .datastoreService(datastoreService)
                        .entityFactory(new EntityFactory(env)))
                .memcache(DbClientBuilder.<Product>memcacheSettings()
                        .memcacheService(memcacheService)
                        .namespace(TABLE_NAME)
                        .jsonTransform(Product.class)
                )
                .build()
        );
    }

    public List<Product> getProductsByCategory(final ProductCategory category) {
        checkNotNull(category);
        checkArgument(category != ProductCategory.UNRECOGNIZED);
        return queryInStream(DbQuery.eq(FIELD_CATEGORY, category.name()))
                .collect(Collectors.toList());
    }

    public Product getByHash(final Product product) {
        return getByHash(product.getCategory(), product.getBrand(), product.getName());
    }

    public Product getByHash(final ProductCategory category, final String brand, final String name) {
        String hash = hash(category, brand, name);
        return queryInStream(DbQuery.eq(FIELD_HASH, hash)).findFirst().orElse(null);
    }

    private static String hash(final ProductCategory category, final String brand, final String name) {
        return EncryptUtils.base64Encode(Product.newBuilder()
                .setCategory(category)
                .setBrand(StringUtils.lowerCase(brand))
                .setName(StringUtils.lowerCase(name))
                .build()
                .toByteString()
                .toStringUtf8());
    }

    private static class EntityFactory extends BaseEntityFactory<Product> {
        EntityFactory(Env env) {
            super(env);
        }

        @Override
        public KeyType getKeyType() {
            return KeyType.LONG_ID;
        }

        @Override
        public Product fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, Product.parser())
                    .toBuilder()
                    .setId(entity.getKeyLongId())
                    .build();
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, Product obj) {
            return partialBuilder.unindexedProto(FIELD_DATA, obj)
                    .indexedString(FIELD_CATEGORY, obj.getCategory().name())
                    .indexedString(FIELD_HASH, hash(obj.getCategory(), obj.getBrand(), obj.getName()))
                    .unindexedString(FIELD_BRAND, obj.getBrand())
                    .unindexedString(FIELD_NAME, obj.getName())
                    .unindexedLastUpdatedTimestampAsNow()
                    .build();
        }

        @Override
        public String getId(Product obj) {
            return obj.getId();
        }

        @Override
        public Product mergeId(Product obj, String id) {
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
