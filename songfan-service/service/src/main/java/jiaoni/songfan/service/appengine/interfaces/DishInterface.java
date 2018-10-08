package jiaoni.songfan.service.appengine.interfaces;

import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.songfan.service.appengine.impls.DishDbClient;
import jiaoni.songfan.wiremodel.entity.Dish;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.stream.Collectors;
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

@Path("/api/dishes")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class DishInterface {
    private final DishDbClient dbClient;

    @Inject
    public DishInterface(final DishDbClient dbClient) {
        this.dbClient = dbClient;
    }

    @POST
    public Response create(final Dish dish) {
        RequestValidator.validateNotNull(dish);
        RequestValidator.validateEmpty(dish.getId());

        Dish afterSave = dbClient.put(dish);
        return Response.ok(afterSave).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") final String id) {
        Dish dish = dbClient.getById(id);
        if (dish == null) {
            throw new NotFoundException();
        }
        return Response.ok(dish).build();
    }

    @GET
    public Response getAll() {
        List<Dish> combos = dbClient.scan().collect(Collectors.toList());
        return Response.ok(combos).build();
    }
}
