package jiaoni.songfan.service.appengine.interfaces.admin;

import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.songfan.service.appengine.impls.OrderDbClient;
import jiaoni.songfan.wiremodel.entity.Order;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/admin/orders")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class AdminOrderInterface {
    private final OrderDbClient orderDbClient;

    @Inject
    public AdminOrderInterface(final OrderDbClient orderDbClient) {
        this.orderDbClient = orderDbClient;
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") final String id) {
        RequestValidator.validateNotBlank(id);
        Order order = orderDbClient.getById(id);
        if (order == null) {
            throw new NotFoundException();
        }
        return Response.ok(order).build();
    }
}
