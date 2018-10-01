package jiaonidaigou.appengine.integrationtest;

import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;
import jiaonidaigou.appengine.wiremodel.entity.PhoneNumber;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CustomerIntegrationTest {
    private final ApiClient apiClient = new ApiClient(Env.DEV);

    @Test
    public void testGetCustomers() throws Exception {
        PaginatedResults<Customer> firstPage = apiClient.newTarget()
                .path("api/customers/JiaoNiDaiGou/getAll")
                .queryParam("limit", 100)
                .request()
                .header(AUTHORIZATION, apiClient.getGoogleAuthTokenBearerHeader())
                .get()
                .readEntity(new GenericType<PaginatedResults<Customer>>() {
                });
        assertEquals(100, firstPage.getResults().size());

        PaginatedResults<Customer> secondPage = apiClient.newTarget()
                .path("api/customers/JiaoNiDaiGou/getAll")
                .queryParam("limit", 100)
                .queryParam("nextToken", firstPage.getPageToken())
                .request()
                .header(AUTHORIZATION, apiClient.getGoogleAuthTokenBearerHeader())
                .get()
                .readEntity(new GenericType<PaginatedResults<Customer>>() {
                });
        assertEquals(100, secondPage.getResults().size());
    }

    @Test
    public void test_put_get_delete() {
        // Create
        Customer beforeCreate = Customer.newBuilder()
                .setName("tom")
                .setPhone(PhoneNumber.newBuilder().setCountryCode("86").setPhone("1234567890").build())
                .build();

        Customer afterCreate = apiClient.newTarget()
                .path("api/customers/JiaoNiDaiGou/put")
                .request()
                .header(AUTHORIZATION, apiClient.getGoogleAuthTokenBearerHeader())
                .put(Entity.json(beforeCreate))
                .readEntity(Customer.class);
        String id = afterCreate.getId();
        assertNotNull(id);

        // Get
        Customer fetchedCustomer = apiClient.newTarget()
                .path("api/customers/JiaoNiDaiGou/get/" + afterCreate.getId())
                .request()
                .header(AUTHORIZATION, apiClient.getGoogleAuthTokenBearerHeader())
                .get()
                .readEntity(Customer.class);
        assertEquals(afterCreate, fetchedCustomer);

        // Update
        Customer beforeUpdate = fetchedCustomer.toBuilder()
                .setIdCard("idcard")
                .build();
        Customer afterUpdate = apiClient.newTarget()
                .path("api/customers/JiaoNiDaiGou/put")
                .request()
                .header(AUTHORIZATION, apiClient.getGoogleAuthTokenBearerHeader())
                .put(Entity.json(beforeUpdate))
                .readEntity(Customer.class);
        assertEquals(beforeUpdate, afterUpdate);

        // Delete
        apiClient.newTarget()
                .path("api/customers/JiaoNiDaiGou/delete/" + id)
                .request()
                .header(AUTHORIZATION, apiClient.getGoogleAuthTokenBearerHeader())
                .delete();

        Response response = apiClient.newTarget()
                .path("api/customers/JiaoNiDaiGou/get/" + afterCreate.getId())
                .request()
                .header(AUTHORIZATION, apiClient.getGoogleAuthTokenBearerHeader())
                .get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
