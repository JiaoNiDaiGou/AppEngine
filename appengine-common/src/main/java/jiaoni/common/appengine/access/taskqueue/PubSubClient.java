package jiaoni.common.appengine.access.taskqueue;

public interface PubSubClient {
    void submit(final QueueName name, final TaskMessage taskMessage);

    void submit(final QueueName name, final long countdownMills, final TaskMessage taskMessage);

    enum QueueName {
        PROD_QUEUE,
        DEV_QUEUE;

        public String queueName() {
            return name().toLowerCase().replace('_', '-');
        }
    }
}
