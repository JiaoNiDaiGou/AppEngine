package jiaoni.daigou.service.integrationtest;

import jiaoni.common.model.Env;
import jiaoni.daigou.tools.remote.ApiClient;
import jiaoni.common.wiremodel.Address;
import jiaoni.daigou.wiremodel.entity.Customer;
import jiaoni.common.wiremodel.PhoneNumber;
import jiaoni.wiremodel.common.entity.PaginatedResults;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import static jiaoni.daigou.tools.remote.ApiClient.CUSTOM_SECRET_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CustomerIntegrationTest {
    private final ApiClient apiClient = new ApiClient(Env.DEV);

    @Test
    public void testGetCustomers() {
        int pageSize = 2;

        PaginatedResults<Customer> firstPage = apiClient.newTarget()
                .path("api/customers/all")
                .queryParam("limit", pageSize)
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<PaginatedResults<Customer>>() {
                });
        assertEquals(pageSize, firstPage.getResults().size());

        PaginatedResults<Customer> secondPage = apiClient.newTarget()
                .path("api/customers/all")
                .queryParam("limit", pageSize)
                .queryParam("nextToken", firstPage.getPageToken())
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<PaginatedResults<Customer>>() {
                });
        assertEquals(pageSize, secondPage.getResults().size());
    }

    @Test
    public void testSetDefaultAddress() {
        // Add
        Customer afterCreate = apiClient.newTarget()
                .path("api/customers/create")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(Customer.newBuilder()
                        .setName(UUID.randomUUID().toString())
                        .setPhone(PhoneNumber.newBuilder().setCountryCode("86").setPhone("1234567890").build())
                        .addAddresses(Address.newBuilder().setRegion("r1"))
                        .addAddresses(Address.newBuilder().setRegion("r2"))
                        .build()))
                .readEntity(Customer.class);
        String id = afterCreate.getId();
        assertTrue(StringUtils.isNotBlank(id));
        assertEquals(0, afterCreate.getDefaultAddressIndex());

        Customer fetched = apiClient.newTarget()
                .path("api/customers/" + id)
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(Customer.class);
        assertEquals(0, fetched.getDefaultAddressIndex());

        Customer afterSetDefaultAddress = apiClient.newTarget()
                .path("api/customers/" + id + "/setDefaultAddress")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(afterCreate.getAddresses(1)))
                .readEntity(Customer.class);
        assertEquals(1, afterSetDefaultAddress.getDefaultAddressIndex());

        Customer fetchedAgain = apiClient.newTarget()
                .path("api/customers/" + id)
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(Customer.class);
        assertEquals(1, fetchedAgain.getDefaultAddressIndex());
    }

    @Test
    public void test_add_address() {
        // Create
        String name = UUID.randomUUID().toString();
        PhoneNumber phoneNumber = PhoneNumber.newBuilder().setCountryCode("86").setPhone("1234567890").build();

        Customer toCreate = Customer.newBuilder()
                .setName(name)
                .setPhone(phoneNumber)
                .addAddresses(Address.newBuilder().setRegion("r1"))
                .build();

        Customer afterCreate = apiClient.newTarget()
                .path("api/customers/create")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(toCreate))
                .readEntity(Customer.class);
        String id = afterCreate.getId();
        assertEquals(1, afterCreate.getAddressesCount());

        // Add address
        Customer toCreateAgain = Customer.newBuilder()
                .setName(name)
                .setPhone(phoneNumber)
                .addAddresses(Address.newBuilder().setRegion("r2"))
                .build();

        Customer afterCreateAgain = apiClient.newTarget()
                .path("api/customers/create")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(toCreateAgain))
                .readEntity(Customer.class);

        assertEquals(2, afterCreateAgain.getAddressesCount());
        assertEquals(Arrays.asList("r2", "r1"), afterCreateAgain.getAddressesList().stream().map(Address::getRegion).collect(Collectors.toList()));

        // Get
        Customer fetchedCustomer = apiClient.newTarget()
                .path("api/customers/" + id)
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(Customer.class);
        assertEquals(afterCreateAgain, fetchedCustomer);
    }

    @Test
    public void test_create_get_delete() {
        // Create
        Customer toCreate = Customer.newBuilder()
                .setName(UUID.randomUUID().toString())
                .setPhone(PhoneNumber.newBuilder().setCountryCode("86").setPhone("1234567890").build())
                .build();

        Customer afterCreate = apiClient.newTarget()
                .path("api/customers/create")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(toCreate))
                .readEntity(Customer.class);
        String id = afterCreate.getId();
        assertNotNull(id);

        // Get
        Customer fetched = apiClient.newTarget()
                .path("api/customers/" + afterCreate.getId())
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(Customer.class);
        assertEquals(afterCreate, fetched);

        // Update
        Customer toUpdate = fetched.toBuilder()
                .setIdCard("idcard")
                .build();
        Customer afterUpdate = apiClient.newTarget()
                .path("api/customers/update")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(toUpdate))
                .readEntity(Customer.class);
        assertEquals(toUpdate.toBuilder().clearLastUpdatedTime().build(),
                afterUpdate.toBuilder().clearLastUpdatedTime().build());

        // Delete
        apiClient.newTarget()
                .path("api/customers/" + id + "/delete")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .delete();

        Response response = apiClient.newTarget()
                .path("api/customers/" + afterCreate.getId())
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
