package jiaoni.daigou.service.appengine.interfaces;

import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.daigou.service.appengine.impls.customer.CustomerFacade;
import jiaoni.daigou.service.appengine.impls.db.v2.CustomerDbClient;
import jiaoni.daigou.v2.entity.Address;
import jiaoni.daigou.v2.entity.Customer;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/customers")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed( {Roles.ADMIN})
public class CustomerInterface {
    private final CustomerFacade customerFacade;
    private CustomerDbClient customerDbClient;

    @Inject
    public CustomerInterface(final CustomerFacade customerFacade) {
        this.customerFacade = customerFacade;
    }

    @GET
    @Path("/all")
    public Response getAllCustomer() {
        List<Customer> customers = customerFacade.getAllCustomers();
        return Response.ok(customers).build();
    }

    @GET
    @Path("/{id}")
    public Response getCustomerById(@PathParam("id") final String id) {
        RequestValidator.validateNotBlank(id);
        Customer customer = customerFacade.getCustomerById(id);
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

        Customer toReturn = customerFacade.createOrUpdateCustomer(customer);

        return Response.ok(toReturn).build();
    }

    /**
     * This is used by delete address
     */
    @POST
    @Path("/update")
    public Response updateCustomer(final Customer customer) {
        RequestValidator.validateNotNull(customer);
        RequestValidator.validateNotBlank(customer.getId());

//        Customer afterSave = customerDbClient.putAndUpdateTimestamp(customer);
//        return Response.ok(afterSave).build();
        return null;
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
//        toReturn = customerDbClient.putAndUpdateTimestamp(toReturn);
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
