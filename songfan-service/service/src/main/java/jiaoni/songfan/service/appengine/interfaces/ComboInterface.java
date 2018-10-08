package jiaoni.songfan.service.appengine.interfaces;

import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.songfan.service.appengine.impls.ComboDbClient;
import jiaoni.songfan.wiremodel.entity.Combo;
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

@Path("/api/combos")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class ComboInterface {
    private final ComboDbClient dbClient;

    @Inject
    public ComboInterface(final ComboDbClient dbClient) {
        this.dbClient = dbClient;
    }

    @POST
    public Response create(final Combo combo) {
        RequestValidator.validateNotNull(combo);
        RequestValidator.validateEmpty(combo.getId());

        Combo afterSave = dbClient.put(combo);
        return Response.ok(afterSave).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") final String id) {
        Combo combo = dbClient.getById(id);
        if (combo == null) {
            throw new NotFoundException();
        }
        return Response.ok(combo).build();
    }

    @GET
    public Response getAll() {
        List<Combo> combos = dbClient.scan().collect(Collectors.toList());
        return Response.ok(combos).build();
    }
}
