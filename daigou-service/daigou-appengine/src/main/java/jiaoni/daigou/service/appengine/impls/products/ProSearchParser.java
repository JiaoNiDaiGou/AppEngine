package jiaoni.daigou.service.appengine.impls.products;

import jiaoni.common.appengine.access.productsearch.ProSearchClient;
import jiaoni.daigou.service.appengine.impls.db.ProductDbClient;
import jiaoni.daigou.wiremodel.api.ParsedObject;
import jiaoni.daigou.wiremodel.entity.Product;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProSearchParser {

    private ProSearchClient searchClient;
    private ProductDbClient dbClient;

    @Inject
    public ProSearchParser(final ProSearchClient searchClient,
                           final ProductDbClient dbClient) {
        this.searchClient = searchClient;
        this.dbClient = dbClient;
    }

    public List<ParsedObject> parse(final String mediaId, final int limit) {
        Map<String, Double> searchResults = searchClient.search(mediaId);
        List<String> topResults = searchResults.entrySet()
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<String, Product> products = dbClient.getByIds(topResults);

        return topResults.stream()
                .map(products::get)
                .filter(Objects::nonNull)
                .map(t -> ParsedObject.newBuilder().setProduct(t).build())
                .collect(Collectors.toList());
    }
}
