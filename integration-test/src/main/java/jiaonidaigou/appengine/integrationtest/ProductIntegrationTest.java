package jiaonidaigou.appengine.integrationtest;

import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import jiaonidaigou.appengine.wiremodel.entity.Product;
import jiaonidaigou.appengine.wiremodel.entity.ProductCategory;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

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
                .post(Entity.entity(product, MediaType.APPLICATION_JSON))
                .readEntity(Product.class);

        System.out.println(ObjectMapperProvider.prettyToJson(product));
    }
}
