package jiaonidaigou.appengine.api.access.taskqueue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import jiaonidaigou.appengine.api.tasks.TaskMessage;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.model.InternalIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class TaskQueueClient implements PubSubClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskQueueClient.class);

    private static final int DEFAULT_RETRY_LIMIT = 3;

    @Override
    public void submit(final QueueName name, final TaskMessage taskMessage) {
        checkNotNull(taskMessage);
        Queue queue = QueueFactory.getQueue(name.queueName());
        checkNotNull(queue);

        LOGGER.info("Send task queue {}. payload: {}", name, taskMessage);

        byte[] payload;
        try {
            payload = ObjectMapperProvider.get().writeValueAsBytes(taskMessage);
        } catch (JsonProcessingException e) {
            throw new InternalIOException(e);
        }

        TaskOptions options = TaskOptions.Builder
                .withTaskName(UUID.randomUUID().toString())
                .url("/tasks/" + taskMessage.getHandler())
                .method(TaskOptions.Method.POST)
                .payload(payload)
                .retryOptions(RetryOptions.Builder.withTaskRetryLimit(DEFAULT_RETRY_LIMIT));

        queue.add(options);
    }
}
