package jiaonidaigou.appengine.api.access.taskqueue;

import jiaonidaigou.appengine.api.tasks.TaskMessage;

public interface PubSubClient {
    enum QueueName {
        HIGH_FREQUENCY,
        LOW_FREQUENCY;

        public String queueName() {
            return name().toLowerCase().replace('_', '-');
        }
    }

    void submit(final QueueName name, final TaskMessage taskMessage);
}
