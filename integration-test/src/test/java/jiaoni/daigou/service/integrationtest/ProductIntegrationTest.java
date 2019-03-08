package jiaoni.daigou.service.integrationtest;

import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.Env;
import jiaoni.common.test.ApiClient;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.daigou.wiremodel.entity.ProductCategory;
import jiaoni.daigou.wiremodel.entity.ProductHints;
import org.junit.Test;

import javax.ws.rs.client.Entity;

import static org.junit.Assert.assertTrue;

public class ProductIntegrationTest {
    private final ApiClient apiClient = new ApiClient(AppEnvs.getHostname(Env.DEV));

    @Test
    public void test_put_get() {
        Product product = Product.newBuilder()
                .setBrand("brand")
                .setCategory(ProductCategory.BAGS)
                .setName("name")
                .build();

        product = apiClient.newTarget()
                .path("/api/products/create")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(product))
                .readEntity(Product.class);

        System.out.println(ObjectMapperProvider.prettyToJson(product));
    }

    @Test
    public void testGetProductsHints() {
        ProductHints hints = apiClient.newTarget()
                .path("/api/products/hints")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(ProductHints.class);
        assertTrue(hints.getHintsList().size() > 0);
        System.out.println(ObjectMapperProvider.prettyToJson(hints));
    }
}
