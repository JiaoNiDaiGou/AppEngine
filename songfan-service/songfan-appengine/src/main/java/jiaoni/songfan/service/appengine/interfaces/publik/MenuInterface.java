package jiaoni.songfan.service.appengine.interfaces.publik;

import jiaoni.songfan.service.appengine.impls.MenuDbClient;
import jiaoni.songfan.wiremodel.entity.Menu;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/menus")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@PermitAll
public class MenuInterface {
    private final MenuDbClient dbClient;

    @Inject
    public MenuInterface(final MenuDbClient dbClient) {
        this.dbClient = dbClient;
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") final String id) {
        Menu menu = dbClient.getById(id);
        if (menu == null) {
            throw new NotFoundException();
        }
        return Response.ok(menu).build();
    }
}
