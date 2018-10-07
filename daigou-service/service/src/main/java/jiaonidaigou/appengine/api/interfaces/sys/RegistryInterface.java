package jiaonidaigou.appengine.api.interfaces.sys;

import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaonidaigou.appengine.api.utils.RegistryFactory;
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

@Path("/api/sys/registry")
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

        RegistryFactory.get().setRegistry(serviceName, keyName, value);

        return Response.ok(value).build();
    }

    @GET
    @Path("/get/{serviceName}/{keyName}")
    public Response get(@QueryParam("serviceName") final String serviceName,
                        @QueryParam("keyName") final String keyName) {
        RequestValidator.validateNotBlank(serviceName);
        RequestValidator.validateNotBlank(keyName);

        String value = RegistryFactory.get().getRegistry(serviceName, keyName);

        return Response.ok(value != null ? value : "").build();
    }
}
