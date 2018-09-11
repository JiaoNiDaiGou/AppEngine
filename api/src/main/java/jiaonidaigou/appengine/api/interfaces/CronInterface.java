package jiaonidaigou.appengine.api.interfaces;

import jiaonidaigou.appengine.api.auth.Roles;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/cron")
@Produces(MediaType.APPLICATION_JSON)
@Service
@RolesAllowed({ Roles.ADMIN, Roles.SYS_TASK_QUEUE_OR_CRON })
public class CronInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(CronInterface.class);

    @Inject
    public CronInterface() {
    }
}
