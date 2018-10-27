package jiaoni.daigou.service.integrationtest;

import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.Env;
import jiaoni.common.test.ApiClient;
import jiaoni.common.wiremodel.Address;
import jiaoni.common.wiremodel.Price;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.wiremodel.api.InitShippingOrderRequest;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.daigou.wiremodel.entity.ProductCategory;
import jiaoni.daigou.wiremodel.entity.ShippingOrder;
import jiaoni.wiremodel.common.entity.PaginatedResults;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;

import static org.junit.Assert.assertEquals;

public class ShippingOrderIntegrationTest {
    private final ApiClient apiClient = new ApiClient(AppEnvs.getHostname(Env.DEV));

    private static final String KNOWN_CUSTOMER_ID = "ODZ8MTIzNDU2Nzg5MHw0MTk5NDk0NC1jNjJmLTRkZmEtOTVlZi0wMWU4N2QwNTY3NmU=";

    @Test
    public void test_query_nonDelivered_byCustomer() {
        PaginatedResults<ShippingOrder> results = apiClient.newTarget()
                .path("api/shippingOrders/query")
                .queryParam("customerName", "王海亚")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<PaginatedResults<ShippingOrder>>() {
                });
        System.out.println(ObjectMapperProvider.prettyToJson(results));
    }

    @Test
    public void test_query_delivered_byCustomer() {
        PaginatedResults<ShippingOrder> results = apiClient.newTarget()
                .path("api/shippingOrders/query")
                .queryParam("customerId", "ODZ8MTM0MDgzNTU0NDh85qiK5Lmm5L2Z")
                .queryParam("includeDelivered", true)
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<PaginatedResults<ShippingOrder>>() {
                });
        System.out.println(ObjectMapperProvider.prettyToJson(results));
    }


    @Test
    public void test_query_byStatus() {
        PaginatedResults<ShippingOrder> results = apiClient.newTarget()
                .path("api/shippingOrders/query")
                .queryParam("status", ShippingOrder.Status.CN_POSTMAN_ASSIGNED.name())
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<PaginatedResults<ShippingOrder>>() {
                });
        System.out.println(ObjectMapperProvider.prettyToJson(results));
    }

    @Test
    public void test_init_get() {
        InitShippingOrderRequest initRequest = InitShippingOrderRequest.newBuilder()
                .setAddress(Address.newBuilder()
                        .setRegion("region")
                        .setCity("city")
                        .setZone("zone")
                        .setAddress("addr"))
                .addProductEntries(ShippingOrder.ProductEntry.newBuilder()
                        .setProduct(Product.newBuilder()
                                .setCategory(ProductCategory.BAGS)
                                .setBrand("brand")
                                .setName("product_name"))
                        .setQuantity(2))
                .addProductEntries(ShippingOrder.ProductEntry.newBuilder()
                        .setProduct(Product.newBuilder()
                                .setCategory(ProductCategory.BAGS)
                                .setBrand("brand")
                                .setName("product_name2"))
                        .setQuantity(20))
                .setReceiverCustomerId(KNOWN_CUSTOMER_ID)
                .setTotalSellPrice(Price.newBuilder().setUnit(Price.Unit.RMB).setValue(12))
                .build();
        ShippingOrder shippingOrder = apiClient.newTarget()
                .path("api/shippingOrders/init")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(initRequest))
                .readEntity(ShippingOrder.class);

        assertEquals(Price.newBuilder().setUnit(Price.Unit.RMB).setValue(12d).build(),
                shippingOrder.getTotalSellPrice());
        assertEquals(2, shippingOrder.getProductEntriesCount());
    }

    @Test
    public void test_get() {
        String shippingOrderId = "5741384237580288";
        ShippingOrder shippingOrder = apiClient.newTarget()
                .path("api/shippingOrders/get/" + shippingOrderId)
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(ShippingOrder.class);

        System.out.println(shippingOrder);
    }
}
