package jiaoni.daigou.service.integrationtest;

import jiaoni.common.model.Env;
import jiaoni.common.test.ApiClient;
import jiaoni.common.wiremodel.Price;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.wiremodel.api.AssignOwnershipShoppingListItemRequest;
import jiaoni.daigou.wiremodel.api.ExpireShoppingListItemRequest;
import jiaoni.daigou.wiremodel.api.InitShoppingListItemRequest;
import jiaoni.daigou.wiremodel.api.PurchaseShoppingListItemRequest;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.daigou.wiremodel.entity.ProductCategory;
import jiaoni.daigou.wiremodel.entity.ShoppingListItem;
import org.junit.Test;

import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;

import static jiaoni.common.test.ApiClient.CUSTOM_SECRET_HEADER;
import static jiaoni.daigou.wiremodel.entity.ShoppingListItem.Status.EXPIRED;
import static jiaoni.daigou.wiremodel.entity.ShoppingListItem.Status.INIT;
import static jiaoni.daigou.wiremodel.entity.ShoppingListItem.Status.IN_HOUSE;
import static jiaoni.daigou.wiremodel.entity.ShoppingListItem.Status.OWNERSHIP_ASSIGNED;
import static jiaoni.daigou.wiremodel.entity.ShoppingListItem.Status.PURCHASED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ShoppingListIntegrationTest {
    private final ApiClient apiClient = new ApiClient(AppEnvs.getHostname(Env.DEV));

    @Test
    public void testInit_GetById() {
        InitShoppingListItemRequest initRequest = InitShoppingListItemRequest.newBuilder()
                .setCreatorName("creator")
                .addProductEntries(ShoppingListItem.ProductEntry.newBuilder()
                        .setProduct(Product.newBuilder()
                                .setName("product_name")
                                .setBrand("product_brand")
                                .setCategory(ProductCategory.ACCESSORIES))
                        .setQuantity(1)
                        .build())
                .build();

        ShoppingListItem afterInit = apiClient.newTarget()
                .path("/api/shoppingLists/init")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(initRequest))
                .readEntity(ShoppingListItem.class);
        assertEquals("creator", afterInit.getCreatorName());
        assertEquals(INIT, afterInit.getStatus());

        ShoppingListItem item = apiClient.newTarget()
                .path("/api/shoppingLists/" + afterInit.getId())
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(ShoppingListItem.class);
        assertEquals(INIT, item.getStatus());
    }

    @Test
    public void testQueryByStatus() {
        InitShoppingListItemRequest initRequest = InitShoppingListItemRequest.newBuilder()
                .setCreatorName("creator")
                .addProductEntries(ShoppingListItem.ProductEntry.newBuilder()
                        .setProduct(Product.newBuilder()
                                .setName("product_name")
                                .setBrand("product_brand")
                                .setCategory(ProductCategory.ACCESSORIES))
                        .setQuantity(1)
                        .build())
                .build();

        ShoppingListItem afterInit = apiClient.newTarget()
                .path("/api/shoppingLists/init")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(initRequest))
                .readEntity(ShoppingListItem.class);
        assertEquals("creator", afterInit.getCreatorName());
        assertEquals(INIT, afterInit.getStatus());

        List<ShoppingListItem> items = apiClient.newTarget()
                .path("/api/shoppingLists/query")
                .queryParam("status", INIT.name())
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<List<ShoppingListItem>>() {
                });
        assertNotNull(items);
    }

    /**
     * init -> expire
     */
    @Test
    public void testExpireCycle() {
        InitShoppingListItemRequest initRequest = InitShoppingListItemRequest.newBuilder()
                .setCreatorName("creator")
                .addProductEntries(ShoppingListItem.ProductEntry.newBuilder()
                        .setProduct(Product.newBuilder()
                                .setName("product_name")
                                .setBrand("product_brand")
                                .setCategory(ProductCategory.ACCESSORIES))
                        .setQuantity(1)
                        .build())
                .build();

        ShoppingListItem afterInit = apiClient.newTarget()
                .path("/api/shoppingLists/init")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(initRequest))
                .readEntity(ShoppingListItem.class);
        assertEquals("creator", afterInit.getCreatorName());
        assertEquals(INIT, afterInit.getStatus());

        final String id = afterInit.getId();

        ExpireShoppingListItemRequest request = ExpireShoppingListItemRequest.newBuilder()
                .setExpireName("expire_name")
                .build();
        apiClient.newTarget()
                .path("/api/shoppingLists/" + id + "/expire")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(request));

        ShoppingListItem item = apiClient.newTarget()
                .path("/api/shoppingLists/" + id)
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(ShoppingListItem.class);
        assertEquals(EXPIRED, item.getStatus());
    }

    /**
     * init -> assign ownership -> purchased -> in hourse -> finished
     */
    @Test
    public void testHappyLifeCycle() {
        // Init
        InitShoppingListItemRequest initRequest = InitShoppingListItemRequest.newBuilder()
                .setCreatorName("creator")
                .addProductEntries(ShoppingListItem.ProductEntry.newBuilder()
                        .setProduct(Product.newBuilder()
                                .setName("product_name")
                                .setBrand("product_brand")
                                .setCategory(ProductCategory.ACCESSORIES))
                        .setQuantity(1)
                        .build())
                .build();

        ShoppingListItem afterInit = apiClient.newTarget()
                .path("/api/shoppingLists/init")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(initRequest))
                .readEntity(ShoppingListItem.class);
        assertEquals("creator", afterInit.getCreatorName());
        assertEquals(INIT, afterInit.getStatus());

        final String id = afterInit.getId();
        System.out.println("new shoppingListItem ID: " + id);

        // Assign ownership
        AssignOwnershipShoppingListItemRequest assignOwnershipRequest = AssignOwnershipShoppingListItemRequest
                .newBuilder()
                .setOwnerName("buyer")
                .build();
        ShoppingListItem afterAssignOwnership = apiClient.newTarget()
                .path("/api/shoppingLists/" + id + "/assign")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(assignOwnershipRequest))
                .readEntity(ShoppingListItem.class);
        assertEquals(OWNERSHIP_ASSIGNED, afterAssignOwnership.getStatus());
        assertEquals("buyer", afterAssignOwnership.getOwnerName());

        // Purchase
        PurchaseShoppingListItemRequest purchaseRequest = PurchaseShoppingListItemRequest
                .newBuilder()
                .setPurchaserName("buyer")
                .setTotalPurchasePrice(Price.newBuilder().setUnit(Price.Unit.USD).setValue(12).build())
                .build();
        ShoppingListItem afterPurchase = apiClient.newTarget()
                .path("/api/shoppingLists/" + id + "/purchase")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(purchaseRequest))
                .readEntity(ShoppingListItem.class);
        assertEquals("buyer", afterPurchase.getPurchaserName());
        assertEquals(PURCHASED, afterPurchase.getStatus());

        // In house
        ShoppingListItem afterInHouse = apiClient.newTarget()
                .path("/api/shoppingLists/" + id + "/inHouse")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json("anything_works"))
                .readEntity(ShoppingListItem.class);
        assertEquals(IN_HOUSE, afterInHouse.getStatus());
    }
}
