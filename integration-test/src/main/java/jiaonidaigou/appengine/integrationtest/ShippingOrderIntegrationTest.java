package jiaonidaigou.appengine.integrationtest;

import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;
import org.junit.Test;

import javax.ws.rs.core.GenericType;

public class ShippingOrderIntegrationTest {
    private final ApiClient client = new ApiClient(Env.PROD);

    @Test
    public void test_query_nonDelivered_byCustomer() {
        PaginatedResults<ShippingOrder> results = client.newTarget()
                .path("api/shippingOrders/query")
//                .queryParam("customerName", "王海亚")
                .queryParam("customerPhone", "13839253898")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, client.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<PaginatedResults<ShippingOrder>>() {
                });
        System.out.println(ObjectMapperProvider.prettyToJson(results));
    }
}
