package jiaoni.daigou.service.appengine.impls.products;

import jiaoni.common.appengine.access.productsearch.ProSearchClient;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.daigou.service.appengine.impls.db.ProductDbClient;
import jiaoni.daigou.wiremodel.entity.Product;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static jiaoni.common.utils.CollectionUtils2.appendNoDup;
import static jiaoni.common.utils.CollectionUtils2.firstNotBlank;
import static jiaoni.common.utils.CollectionUtils2.firstRecognized;

@Singleton
public class ProductFacade {
    private final ProductDbClient dbClient;
//    private final ProSearchClient searchClient;

    @Inject
    public ProductFacade(final ProductDbClient dbClient,
                         final ProSearchClient searchClient) {
        this.dbClient = dbClient;
//        this.searchClient = searchClient;
    }

    public List<Product> getAll() {
        return dbClient.scan().collect(Collectors.toList());
    }

    public Product get(final String id) {
        return dbClient.getById(id);
    }

    public Product create(final Product product) {
        RequestValidator.validateEmpty(product.getId());
        Product toReturn = dbClient.put(product);
//        toReturn = searchClient.addProduct(toReturn, product.getMediaIdsList());
        toReturn = dbClient.put(toReturn);
        return toReturn;
    }

    public List<Product> create(final List<Product> products) {
        checkNotNull(products);
        products.forEach(t -> checkArgument(StringUtils.isBlank(t.getId())));
        return dbClient.put(products);
    }

    public Product update(final Product product) {
        RequestValidator.validateNotBlank(product.getId());

        Product toReturn = dbClient.getById(product.getId());
        if (toReturn == null) {
            return null;
        }
//        List<String> extraMediaIds = new ArrayList<>(CollectionUtils.subtract(product.getMediaIdsList(), toReturn.getMediaIdsList()));
        toReturn = toReturn.toBuilder()
                .setName(firstNotBlank(product.getName(), toReturn.getName()))
                .setBrand(firstNotBlank(product.getBrand(), toReturn.getBrand()))
                .setCategory(firstRecognized(product.getCategory(), toReturn.getCategory()))
                .setDescription(firstNotBlank(product.getDescription(), toReturn.getDescription()))
                .clearMediaIds()
                .addAllMediaIds(appendNoDup(toReturn.getMediaIdsList(), product.getMediaIdsList()))
                .build();
//        toReturn = searchClient.addProduct(toReturn, extraMediaIds);
        toReturn = dbClient.put(toReturn);
        return toReturn;
    }

    public Product attachMedia(final String productId, final List<String> mediaIds) {
        Product toReturn = dbClient.getById(productId);
        if (toReturn == null) {
            return null;
        }
//        toReturn = searchClient.addProduct(toReturn, mediaIds);
        toReturn = toReturn.toBuilder()
                .clearMediaIds()
                .addAllMediaIds(appendNoDup(toReturn.getMediaIdsList(), mediaIds))
                .build();
        toReturn = dbClient.put(toReturn);
        return toReturn;
    }
}
