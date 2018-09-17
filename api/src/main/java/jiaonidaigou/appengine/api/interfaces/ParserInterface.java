package jiaonidaigou.appengine.api.interfaces;

import jiaonidaigou.appengine.api.auth.Roles;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.security.RolesAllowed;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/api/parse")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class ParserInterface {
}
