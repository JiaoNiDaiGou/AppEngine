package jiaoni.songfan.service.appengine.interfaces.admin;

import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.songfan.service.appengine.impls.DishDbClient;
import jiaoni.songfan.wiremodel.entity.Dish;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/admin/dishes")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class AdminDishInterface {
    private final DishDbClient dbClient;

    @Inject
    public AdminDishInterface(final DishDbClient dbClient) {
        this.dbClient = dbClient;
    }

    @POST
    @RolesAllowed({ Roles.ADMIN })
    public Response create(final Dish dish) {
        RequestValidator.validateNotNull(dish);
        RequestValidator.validateEmpty(dish.getId());

        Dish afterSave = dbClient.put(dish);
        return Response.ok(afterSave).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("id") final String id) {
        Dish dish = dbClient.getById(id);
        if (dish == null) {
            throw new NotFoundException();
        }
        return Response.ok(dish).build();
    }

    @GET
    @PermitAll
    public Response getAll() {
        List<Dish> combos = dbClient.scan().collect(Collectors.toList());
        return Response.ok(combos).build();
    }
}
