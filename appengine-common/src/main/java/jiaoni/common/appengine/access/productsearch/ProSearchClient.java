package jiaoni.common.appengine.access.productsearch;

import jiaoni.daigou.wiremodel.entity.Product;

import java.util.List;
import java.util.Map;

public interface ProSearchClient {
    Product addProduct(final Product product);

    Product addProduct(final Product product, final List<String> mediaIds);

    /**
     * Search similar productIds with their confidences.
     */
    Map<String, Double> search(final String mediaId);
}
