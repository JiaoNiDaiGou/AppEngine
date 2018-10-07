package jiaoni.daigou.service.appengine.impls;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaoni.common.appengine.access.db.BaseDbClient;
import jiaoni.common.appengine.access.db.BaseEntityFactory;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DbClientBuilder;
import jiaoni.common.appengine.access.db.DbQuery;
import jiaoni.common.model.Env;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.daigou.wiremodel.entity.ProductCategory;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class ProductDbClient extends BaseDbClient<Product> {
    private static final String FIELD_DATA = "data";
    private static final String FIELD_CATEGORY = "category";

    @Inject
    public ProductDbClient(final DatastoreService datastoreService) {
        this(datastoreService, AppEnvs.getEnv());
    }

    public ProductDbClient(final DatastoreService datastoreService, final Env env) {
        super(new DbClientBuilder<Product>()
                .datastoreService(datastoreService)
                .entityFactory(new EntityFactory(env, "Product"))
                .build());
    }

    public List<Product> getProductsByCategory(final ProductCategory category) {
        checkNotNull(category);
        checkArgument(category != ProductCategory.UNRECOGNIZED);
        return queryInStream(DbQuery.eq(FIELD_CATEGORY, category.name()))
                .collect(Collectors.toList());
    }

    private static class EntityFactory extends BaseEntityFactory<Product> {

        protected EntityFactory(Env env, String tableName) {
            super(AppEnvs.getServiceName(), env, tableName);
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
