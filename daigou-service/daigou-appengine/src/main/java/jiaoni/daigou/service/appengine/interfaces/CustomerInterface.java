package jiaoni.daigou.service.appengine.interfaces;

import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.common.wiremodel.Address;
import jiaoni.daigou.service.appengine.impls.CustomerDbClient;
import jiaoni.daigou.service.appengine.impls.teddy.TeddyWarmUp;
import jiaoni.daigou.wiremodel.entity.Customer;
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

@Path("/api/customers")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class CustomerInterface {
    private final CustomerDbClient customerDbClient;
    private final TeddyWarmUp teddyWarmUp;

    @Inject
    public CustomerInterface(final CustomerDbClient customerDbClient,
                             final TeddyWarmUp teddyWarmUp) {
        this.customerDbClient = customerDbClient;
        this.teddyWarmUp = teddyWarmUp;
    }

    @GET
    @Path("/all")
    public Response getAllCustomer(@Deprecated @QueryParam("nextToken") final String nextToken,
                                   @Deprecated @QueryParam("limit") final int limit) {
        List<Customer> customers = customerDbClient.scan().collect(Collectors.toList());
        teddyWarmUp.warmUpAsyncIfNeeded();
        return Response.ok(customers).build();
    }

    @GET
    @Path("/{id}")
    public Response getAllCustomer(@PathParam("id") final String id) {
        RequestValidator.validateNotBlank(id);

        Customer customer = customerDbClient.getById(id);
        if (customer == null) {
            throw new NotFoundException();
        } else {
            return Response.ok(customer).build();
        }
    }

    /**
     * Create a new customer if not exists, or add address if the customer exists.
     */
    @POST
    @Path("/create")
    public Response createCustomer(final Customer customer) {
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
                .addAllAddresses(newAddresses) // The new address will be the default one.
                .addAllAddresses(existing.getAddressesList())
                .setDefaultAddressIndex(0)
                .build();
        toReturn = customerDbClient.putAndUpdateTimestamp(toReturn);
        return Response.ok(toReturn).build();
    }

    @POST
    @Path("/update")
    public Response updateCustomer(final Customer customer) {
        RequestValidator.validateNotNull(customer);
        RequestValidator.validateNotBlank(customer.getId());

        Customer afterSave = customerDbClient.putAndUpdateTimestamp(customer);
        return Response.ok(afterSave).build();
    }

    @POST
    @Path("/{id}/setDefaultAddress")
    public Response setDefaultAddress(@PathParam("id") final String id,
                                      final Address address) {
        RequestValidator.validateNotBlank(id);

        Customer customer = customerDbClient.getById(id);
        if (customer == null) {
            throw new NotFoundException();
        }

        int defaultAddressIndex = customer.getAddressesList().indexOf(address);

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
    public Response deleteCustomer(@PathParam("id") final String id) {
        RequestValidator.validateNotBlank(id);

        customerDbClient.delete(id);
        return Response.ok().build();
    }
}
