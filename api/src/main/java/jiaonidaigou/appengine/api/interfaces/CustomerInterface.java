package jiaonidaigou.appengine.api.interfaces;

import jiaonidaigou.appengine.api.access.db.CustomerDbClient;
import jiaonidaigou.appengine.api.access.db.core.PageToken;
import jiaonidaigou.appengine.api.auth.Roles;
import jiaonidaigou.appengine.api.guice.JiaoNiDaiGou;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.wiremodel.entity.Address;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/{app}/customers")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class CustomerInterface {
    private static final int DEFAULT_PAGE_LIMIT = 100;

    private final CustomerDbClient customerDbClient;


    @Inject
    public CustomerInterface(@JiaoNiDaiGou final CustomerDbClient customerDbClient) {
        this.customerDbClient = customerDbClient;
    }

    @GET
    @Path("/all")
    public Response getAllCustomer(@PathParam("app") final String appName,
                                   @QueryParam("nextToken") final String nextToken,
                                   @QueryParam("limit") final int limit) {
        RequestValidator.validateAppName(appName);

        PaginatedResults<Customer> results = customerDbClient.queryInPagination(
                limit <= 0 ? DEFAULT_PAGE_LIMIT : limit,
                PageToken.fromPageToken(nextToken));
        return Response.ok(results).build();
    }

    @GET
    @Path("/{id}")
    public Response getAllCustomer(@PathParam("app") final String appName,
                                   @PathParam("id") final String id) {
        RequestValidator.validateNotBlank(appName);
        RequestValidator.validateNotBlank(id);
        RequestValidator.validateAppName(appName);

        Customer customer = customerDbClient.getById(id);
        if (customer == null) {
            throw new NotFoundException();
        } else {
            return Response.ok(customer).build();
        }
    }

    @POST
    @Path("/create")
    public Response createCustomer(@PathParam("app") final String appName,
                                   final Customer customer) {
        RequestValidator.validateValueInSet(appName, Environments.ALL_OPEN_NAMESPACES, appName);
        RequestValidator.validateNotBlank(appName);
        RequestValidator.validateNotNull(customer);

        String key = CustomerDbClient.computeKey(customer.getPhone(), customer.getName());
        Customer existing = customerDbClient.getById(key);
        if (existing == null) {
            Customer toReturn = customerDbClient.putAndUpdateTimestamp(customer.toBuilder().setId(key).build());
            return Response.ok(toReturn).build();
        }
        List<Address> newAddresses = customer.getAddressesList()
                .stream()
                .filter(t -> !existing.getAddressesList().contains(t))
                .collect(Collectors.toList());
        Customer toReturn = existing.toBuilder()
                .clearAddresses()
                .addAllAddresses(newAddresses)
                .addAllAddresses(existing.getAddressesList())
                .build();
        toReturn = customerDbClient.putAndUpdateTimestamp(toReturn);
        return Response.ok(toReturn).build();
    }

    @POST
    @Path("/update")
    public Response updateCustomer(@PathParam("app") final String appName,
                                   final Customer customer) {
        RequestValidator.validateValueInSet(appName, Environments.ALL_OPEN_NAMESPACES, appName);
        RequestValidator.validateNotBlank(appName);
        RequestValidator.validateNotNull(customer);
        RequestValidator.validateNotBlank(customer.getId());

        Customer afterSave = customerDbClient.putAndUpdateTimestamp(customer);
        return Response.ok(afterSave).build();
    }

    @POST
    @Path("/{id}/setDefaultAddress")
    public Response setDefaultAddress(@PathParam("app") final String appName,
                                      @PathParam("id") final String id,
                                      final Address address) {
        RequestValidator.validateNotBlank(appName);
        RequestValidator.validateNotBlank(id);
        RequestValidator.validateAppName(appName);

        Customer customer = customerDbClient.getById(id);
        if (customer == null) {
            throw new NotFoundException();
        }

        int defaultAddressIndex = customer.getAddressesList()
                .indexOf(address);
        Customer toReturn;
        if (defaultAddressIndex >= 0) {
            toReturn = customer.toBuilder()
                    .setDefaultAddressIndex(defaultAddressIndex)
                    .build();
        } else {
            toReturn = customer.toBuilder()
                    .clearAddresses()
                    .addAddresses(address)
                    .addAllAddresses(customer.getAddressesList())
                    .setDefaultAddressIndex(0)
                    .build();
        }
        toReturn = customerDbClient.putAndUpdateTimestamp(toReturn);
        return Response.ok(toReturn).build();
    }

    @DELETE
    @Path("/{id}/delete")
    public Response deleteCustomer(@PathParam("app") final String appName,
                                   @PathParam("id") final String id) {
        RequestValidator.validateNotBlank(id);
        RequestValidator.validateValueInSet(appName, Environments.ALL_OPEN_NAMESPACES, appName);

        customerDbClient.delete(id);
        return Response.ok().build();
    }
}
