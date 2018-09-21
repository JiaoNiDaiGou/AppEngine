package jiaonidaigou.appengine.api.access.taskqueue;

import com.google.common.base.Preconditions;
import jiaonidaigou.appengine.api.tasks.TaskMessage;

import java.util.function.Consumer;

public class LocalStaticTaskClient implements PubSubClient {
    private static Consumer<TaskMessage> currentRunner;

    public static void initialize(final Consumer<TaskMessage> currentRunner) {
        LocalStaticTaskClient.currentRunner = currentRunner;
    }

    @Override
    public void submit(QueueName name, TaskMessage taskMessage) {
        Preconditions.checkNotNull(currentRunner);
        currentRunner.accept(taskMessage);
    }

    public static LocalStaticTaskClient instance() {
        return LazyHolder.instance;
    }

    private LocalStaticTaskClient() {
    }

    private static class LazyHolder {
        private static final LocalStaticTaskClient instance = new LocalStaticTaskClient();
    }
}
