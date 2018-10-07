package jiaonidaigou.appengine.api.access.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaonidaigou.appengine.api.access.db.core.BaseDbClient;
import jiaonidaigou.appengine.api.access.db.core.BaseEntityFactory;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityBuilder;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityExtractor;
import jiaonidaigou.appengine.api.access.db.core.DbClientBuilder;
import jiaonidaigou.appengine.api.access.db.core.DbQuery;
import jiaonidaigou.appengine.api.utils.AppEnvironments;
import jiaoni.common.model.Env;
import jiaonidaigou.appengine.wiremodel.entity.Product;
import jiaonidaigou.appengine.wiremodel.entity.ProductCategory;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ProductDbClient extends BaseDbClient<Product> {
    private static final String FIELD_DATA = "data";
    private static final String FIELD_CATEGORY = "category";

    public ProductDbClient(final DatastoreService datastoreService, final String serviceName) {
        this(datastoreService, serviceName, AppEnvironments.ENV);
    }

    public ProductDbClient(final DatastoreService datastoreService, final String serviceName, final Env env) {
        super(new DbClientBuilder<Product>()
                .datastoreService(datastoreService)
                .entityFactory(new EntityFactory(serviceName, env, "Product"))
                .build());
    }

    public List<Product> getProductsByCategory(final ProductCategory category) {
        checkNotNull(category);
        checkArgument(category != ProductCategory.UNRECOGNIZED);
        return queryInStream(DbQuery.eq(FIELD_CATEGORY, category.name()))
                .collect(Collectors.toList());
    }

    private static class EntityFactory extends BaseEntityFactory<Product> {

        protected EntityFactory(String serviceName, Env env, String tableName) {
            super(serviceName, env, tableName);
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
    }
}
