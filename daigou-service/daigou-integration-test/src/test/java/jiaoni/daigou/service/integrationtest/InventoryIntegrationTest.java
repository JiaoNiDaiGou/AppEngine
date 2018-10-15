package jiaoni.daigou.service.integrationtest;

import jiaoni.common.model.Env;
import jiaoni.common.test.ApiClient;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.wiremodel.entity.InventoryItem;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.daigou.wiremodel.entity.ProductCategory;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.UUID;
import javax.ws.rs.client.Entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InventoryIntegrationTest {
    private final ApiClient apiClient = new ApiClient(AppEnvs.getHostname(Env.DEV));

    @Test
    public void test_create_update() {
        Product product = Product.newBuilder()
                .setBrand("brand")
                .setCategory(ProductCategory.BABY_PRODUCTS)
                .setName("name-" + UUID.randomUUID().toString())
                .build();

        InventoryItem afterCreate = apiClient.newTarget()
                .path("/api/inventory/create")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(
                        InventoryItem.newBuilder()
                                .setProduct(product)
                                .setQuantity(10)
                                .build())
                )
                .readEntity(InventoryItem.class);

        assertNotNull(afterCreate.getProduct().getId());
        assertEquals(10, afterCreate.getQuantity());

        InventoryItem afterUpdate = apiClient.newTarget()
                .path("/api/inventory/update")
                .queryParam("productId", afterCreate.getProduct().getId())
                .queryParam("quantity", 20)
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(""))
                .readEntity(InventoryItem.class);
        assertEquals(afterCreate.getProduct(), afterUpdate.getProduct());
        assertEquals(20, afterUpdate.getQuantity());
    }
}
