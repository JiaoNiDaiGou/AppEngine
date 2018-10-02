package jiaonidaigou.appengine.api.interfaces;

import jiaonidaigou.appengine.api.access.taskqueue.TaskQueueClient;
import jiaonidaigou.appengine.api.auth.Roles;
import jiaonidaigou.appengine.api.tasks.DumpTeddyShippingOrdersTaskRunner;
import jiaonidaigou.appengine.api.tasks.SyncJiaoniCustomersTaskRunner;
import jiaonidaigou.appengine.api.tasks.TaskMessage;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static jiaonidaigou.appengine.api.access.taskqueue.PubSubClient.QueueName.HIGH_FREQUENCY;

@Path("/cron")
@Produces(MediaType.APPLICATION_JSON)
@Service
@RolesAllowed({ Roles.ADMIN, Roles.SYS_TASK_QUEUE_OR_CRON })
public class CronInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(CronInterface.class);

    private final TaskQueueClient taskQueueClient;

    @Inject
    public CronInterface(final TaskQueueClient taskQueueClient) {
        this.taskQueueClient = taskQueueClient;
    }

    @Path("/syncJiaoniCustomers")
    @GET
    public Response syncJiaoniCustomers() {
        taskQueueClient.submit(
                HIGH_FREQUENCY,
                TaskMessage.builder()
                        .withHandler(SyncJiaoniCustomersTaskRunner.class)
                        .build()
        );
        return Response.ok().build();
    }

    @Path("/syncJiaoniShippingOrders")
    @GET
    public Response dumpJiaoniShippingOrders() {
//        taskQueueClient.submit(
//                HIGH_FREQUENCY,
//                TaskMessage.builder()
//                        .withHandler(SyncJiaoniShippingOrdersTaskRunner.class)
//                        .build()
//        );
//        return Response.ok().build();
        return null;
    }

    @Path("/dumpTeddyShippingOrders")
    @GET
    public Response dumpTeddyShippingOrders(@QueryParam("id") final long id,
                                            @QueryParam("limit") final int limit,
                                            @QueryParam("backward") final boolean backward) {
        taskQueueClient.submit(
                HIGH_FREQUENCY,
                TaskMessage.builder()
                        .withHandler(DumpTeddyShippingOrdersTaskRunner.class)
                        .withPayloadJson(new DumpTeddyShippingOrdersTaskRunner.Message(id, limit, backward))
                        .build()
        );
        return Response.ok().build();
    }
}
