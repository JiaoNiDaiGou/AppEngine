package jiaoni.daigou.service.appengine.interfaces;

import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.appengine.access.taskqueue.TaskQueueClient;
import jiaoni.common.appengine.auth.Roles;
import jiaoni.daigou.service.appengine.tasks.BuildProductHintsTaskRunner;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static jiaoni.common.appengine.access.taskqueue.PubSubClient.QueueName.PROD_QUEUE;

@Path("/cron")
@Produces(MediaType.APPLICATION_JSON)
@Service
@RolesAllowed( {Roles.ADMIN, Roles.SYS_TASK_QUEUE_OR_CRON})
public class CronInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(CronInterface.class);

    private final TaskQueueClient taskQueueClient;

    @Inject
    public CronInterface(final TaskQueueClient taskQueueClient) {
        this.taskQueueClient = taskQueueClient;
    }

    @Path("/buildProductHints")
    @GET
    public Response buildProductHints() {
        return sendEmptyTaskMessage(BuildProductHintsTaskRunner.class);
    }

    private Response sendEmptyTaskMessage(final Class<? extends Consumer<TaskMessage>> handleType) {
        taskQueueClient.submit(
                PROD_QUEUE,
                TaskMessage.builder()
                        .withHandler(handleType)
                        .build()
        );
        return Response.ok().build();
    }
}
