package jiaonidaigou.appengine.api.access.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaonidaigou.appengine.api.access.db.core.DatastoreClient;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityBuilder;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityExtractor;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityFactory;
import jiaonidaigou.appengine.wiremodel.entity.Product;
import jiaonidaigou.appengine.wiremodel.entity.ProductCategory;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static jiaonidaigou.appengine.api.utils.AppEnvironments.ENV;
import static jiaonidaigou.appengine.common.utils.Environments.SERVICE_NAME;

@Singleton
public class ProductDbClient extends DatastoreClient<Product> {
    private static final String KIND = SERVICE_NAME + "." + ENV + ".Product";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_CATEGORY = "category";

    private static class EntityFactory implements DatastoreEntityFactory<Product> {
        @Override
        public KeyType getKeyType() {
            return KeyType.LONG_ID;
        }

        @Override
        public String getKind() {
            return KIND;
        }

        @Override
        public Product fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, Product.parser());
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

    @Inject
    public ProductDbClient(final DatastoreService service) {
        super(service, new EntityFactory());
    }

    public List<Product> getProductsByCategory(final ProductCategory category) {
        checkNotNull(category);
        checkArgument(category != ProductCategory.UNRECOGNIZED);
        return queryInStream(DbQuery.eq(FIELD_CATEGORY, category.name()))
                .collect(Collectors.toList());
    }
}
