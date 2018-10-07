package jiaonidaigou.appengine.integrationtest;

import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.Env;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;
import org.junit.Test;

import javax.ws.rs.core.GenericType;

public class ShippingOrderIntegrationTest {
    private final ApiClient client = new ApiClient(Env.DEV);

    @Test
    public void test_query_nonDelivered_byCustomer() {
        PaginatedResults<ShippingOrder> results = client.newTarget()
                .path("api/shippingOrders/query")
                .queryParam("customerName", "王海亚")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, client.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<PaginatedResults<ShippingOrder>>() {
                });
        System.out.println(ObjectMapperProvider.prettyToJson(results));
    }

    @Test
    public void test_query_delivered_byCustomer() {
        PaginatedResults<ShippingOrder> results = client.newTarget()
                .path("api/shippingOrders/query")
                .queryParam("customerId", "ODZ8MTM0MDgzNTU0NDh85qiK5Lmm5L2Z")
                .queryParam("includeDelivered", true)
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, client.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<PaginatedResults<ShippingOrder>>() {
                });
        System.out.println(ObjectMapperProvider.prettyToJson(results));
    }



    @Test
    public void test_query_byStatus() {
        PaginatedResults<ShippingOrder> results = client.newTarget()
                .path("api/shippingOrders/query")
                .queryParam("status", ShippingOrder.Status.CN_POSTMAN_ASSIGNED.name())
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, client.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<PaginatedResults<ShippingOrder>>() {
                });
        System.out.println(ObjectMapperProvider.prettyToJson(results));
    }
}
