package jiaonidaigou.appengine.api.interfaces;

import jiaonidaigou.appengine.api.auth.Roles;
import jiaonidaigou.appengine.api.registry.Registry;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.security.RolesAllowed;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class RegistryInterface {
    @POST
    @Path("/put/{serviceName}/{keyName}")
    public Response put(@QueryParam("serviceName") final String serviceName,
                        @QueryParam("keyName") final String keyName,
                        final String value) {
        RequestValidator.validateNotBlank(serviceName);
        RequestValidator.validateNotBlank(keyName);

        Registry.instance().setRegistry(serviceName, keyName, value);

        return Response.ok(value).build();
    }

    @GET
    @Path("/get/{serviceName}/{keyName}")
    public Response get(@QueryParam("serviceName") final String serviceName,
                        @QueryParam("keyName") final String keyName) {
        RequestValidator.validateNotBlank(serviceName);
        RequestValidator.validateNotBlank(keyName);

        String value = Registry.instance().getRegistry(serviceName, keyName);

        return Response.ok(value != null ? value : "").build();
    }
}
