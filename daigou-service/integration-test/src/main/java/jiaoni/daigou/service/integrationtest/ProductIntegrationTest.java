package jiaoni.daigou.service.integrationtest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.Env;
import jiaoni.daigou.tools.remote.ApiClient;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.daigou.wiremodel.entity.ProductCategory;
import org.junit.Test;

import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;

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
