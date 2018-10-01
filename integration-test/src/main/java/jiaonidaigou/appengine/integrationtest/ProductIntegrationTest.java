package jiaonidaigou.appengine.integrationtest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Table;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import jiaonidaigou.appengine.wiremodel.entity.Product;
import jiaonidaigou.appengine.wiremodel.entity.ProductCategory;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertTrue;

public class ProductIntegrationTest {
    private final ApiClient apiClient = new ApiClient(Env.DEV);

    @Test
    public void test_put_get() {
        Product product = Product.newBuilder()
                .setBrand("brand")
                .setCategory(ProductCategory.BAGS)
                .setName("name")
                .build();

        product = apiClient.newTarget()
                .path("/api/JiaoNiDaiGou/products/create")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(product))
                .readEntity(Product.class);

        System.out.println(ObjectMapperProvider.prettyToJson(product));
    }

    @Test
    public void testGetProductsHints() {
        List<MyTriple> hints = apiClient.newTarget()
                .path("/api/JiaoNiDaiGou/products/hints")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<List<MyTriple>>() {
                });
        assertTrue(hints.size() > 0);
        System.out.println(ObjectMapperProvider.prettyToJson(hints));
    }
    private static class MyTriple {
        @JsonProperty
        ProductCategory left;
        @JsonProperty
        String middle;
        @JsonProperty
        List<String> right;
    }
}
