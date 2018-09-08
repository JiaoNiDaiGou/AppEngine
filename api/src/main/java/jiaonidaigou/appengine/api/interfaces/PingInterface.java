package jiaonidaigou.appengine.api.interfaces;

import jiaonidaigou.appengine.api.auth.Roles;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/ping")
@Produces(MediaType.APPLICATION_JSON)
@Service
public class PingInterface {
    @GET
    @PermitAll
    public Response ping(@QueryParam("input") final String text) {
        return Response.ok("pong: " + text).build();
    }

    @GET
    @Path("/secure")
    @RolesAllowed({ Roles.ADMIN })
    public Response securePing(@QueryParam("input") final String text) {
        return Response.ok("secure pong: " + text).build();
    }
}
