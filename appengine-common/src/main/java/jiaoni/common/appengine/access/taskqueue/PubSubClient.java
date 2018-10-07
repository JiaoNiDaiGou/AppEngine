package jiaoni.common.appengine.access.taskqueue;

public interface PubSubClient {
    void submit(final QueueName name, final TaskMessage taskMessage);

    enum QueueName {
        HIGH_FREQUENCY,
        LOW_FREQUENCY;

        public String queueName() {
            return name().toLowerCase().replace('_', '-');
        }
    }
}
