package jiaonidaigou.appengine.api.interfaces;

import com.google.common.collect.ImmutableMap;
import jiaonidaigou.appengine.api.auth.Roles;
import jiaonidaigou.appengine.api.tasks.BuildProductHintsTaskRunner;
import jiaonidaigou.appengine.api.tasks.DumpTeddyShippingOrdersTaskRunner;
import jiaonidaigou.appengine.api.tasks.SyncJiaoniCustomersTaskRunner;
import jiaonidaigou.appengine.api.tasks.SyncJiaoniShippingOrdersTaskRunner;
import jiaonidaigou.appengine.api.tasks.TaskMessage;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Service
@RolesAllowed({ Roles.ADMIN, Roles.SYS_TASK_QUEUE_OR_CRON })
public class TaskQueueInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskQueueInterface.class);

    private final Map<String, Consumer<TaskMessage>> consumers;

    @Inject
    public TaskQueueInterface(final SyncJiaoniCustomersTaskRunner syncJiaoniCustomersTaskRunner,
                              final DumpTeddyShippingOrdersTaskRunner dumpJiaoniShippingOrderTaskRunner,
                              final SyncJiaoniShippingOrdersTaskRunner syncJiaoniShippingOrdersTaskRunner,
                              final BuildProductHintsTaskRunner buildProductHintsTaskRunner) {
        this.consumers = buildConsumerMap(
                syncJiaoniCustomersTaskRunner,
                dumpJiaoniShippingOrderTaskRunner,
                syncJiaoniShippingOrdersTaskRunner,
                buildProductHintsTaskRunner
        );
        LOGGER.info("Register following Tasks: {}", consumers.keySet());
    }

    @POST
    @Path("/{taskName}")
    public Response simpleTask(@PathParam("taskName") final String taskName,
                               final byte[] body)
            throws IOException {
        TaskMessage taskMessage = ObjectMapperProvider
                .get()
                .readValue(body, TaskMessage.class);
        Consumer<TaskMessage> taskConsumer = consumers.get(taskMessage.getHandler());
        if (taskConsumer == null) {
            LOGGER.warn("Unknown task {}", taskMessage);
            // TODO
            // We may define status code for task queue.
            return Response.ok().build();
        }
        taskConsumer.accept(taskMessage);
        return Response.ok().build();
    }

    private static Map<String, Consumer<TaskMessage>> buildConsumerMap(final Consumer<TaskMessage>... consumers) {
        ImmutableMap.Builder<String, Consumer<TaskMessage>> builder = ImmutableMap.builder();
        for (Consumer<TaskMessage> consumer : consumers) {
            builder.put(consumer.getClass().getSimpleName(), consumer);
        }
        return builder.build();
    }
}

