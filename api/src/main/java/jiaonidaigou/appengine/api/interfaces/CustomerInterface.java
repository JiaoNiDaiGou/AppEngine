package jiaonidaigou.appengine.api.interfaces;

import jiaonidaigou.appengine.api.access.db.CustomerDbClient;
import jiaonidaigou.appengine.api.access.db.core.PageToken;
import jiaonidaigou.appengine.api.auth.Roles;
import jiaonidaigou.appengine.api.guice.JiaoNiDaiGou;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
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
    private static final int DEFAULT_PAGE_LIMIT = 100;

    private final CustomerDbClient customerDbClient;


    @Inject
    public CustomerInterface(@JiaoNiDaiGou final CustomerDbClient customerDbClient) {
        this.customerDbClient = customerDbClient;
    }

    @GET
    @Path("/{app}/getAll")
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
    @Path("/{app}/get/{id}")
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

    @PUT
    @Path("/{app}/put")
    public Response putCustomer(@PathParam("app") final String appName,
                                final Customer customer) {
        RequestValidator.validateValueInSet(appName, Environments.ALL_OPEN_NAMESPACES, appName);
        RequestValidator.validateNotBlank(appName);
        RequestValidator.validateNotNull(customer);

        Customer toSave = customer;
        if (StringUtils.isBlank(customer.getId())) {
            toSave = customer.toBuilder().setId(CustomerDbClient.computeKey(customer.getPhone(), customer.getName())).build();
        }
        Customer afterSave = customerDbClient.put(toSave);
        return Response.ok(afterSave).build();
    }

    @DELETE
    @Path("/{app}/delete/{id}")
    public Response deleteCustomer(@PathParam("app") final String appName,
                                   @PathParam("id") final String id) {
        RequestValidator.validateNotBlank(id);
        RequestValidator.validateValueInSet(appName, Environments.ALL_OPEN_NAMESPACES, appName);

        customerDbClient.delete(id);
        return Response.ok().build();
    }
}
