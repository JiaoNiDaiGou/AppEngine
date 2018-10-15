package jiaoni.daigou.service.appengine.interfaces;

import jiaoni.common.appengine.auth.Roles;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/ping")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
public class PingInterface {
    @GET
    @PermitAll
    public Response ping(@QueryParam("input") final String text) {
        return Response.ok("pong: " + text).build();
    }

    @GET
    @Path("/secure/google")
    @RolesAllowed({ Roles.ADMIN })
    public Response securePingGoogle(@Context ContainerRequestContext context,
                                     @QueryParam("input") final String text) {
        return Response.ok(String.format("secure pong from %s: %s", getCaller(context), text)).build();
    }

    @GET
    @Path("/secure/customSecret")
    @RolesAllowed({ Roles.ADMIN })
    public Response securePingCustomSecret(@Context ContainerRequestContext context,
                                           @QueryParam("input") final String text) {
        return Response.ok(String.format("secure pong from %s: %s", getCaller(context), text)).build();
    }

    private String getCaller(final ContainerRequestContext context) {
        return context.getSecurityContext().getUserPrincipal().getName();
    }
}
