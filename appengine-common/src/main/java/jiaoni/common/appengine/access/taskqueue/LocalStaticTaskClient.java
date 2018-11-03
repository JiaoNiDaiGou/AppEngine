package jiaoni.common.appengine.access.taskqueue;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Uninterruptibles;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LocalStaticTaskClient implements PubSubClient {
    private static Consumer<TaskMessage> currentRunner;

    private LocalStaticTaskClient() {
    }

    public static void initialize(final Consumer<TaskMessage> currentRunner) {
        LocalStaticTaskClient.currentRunner = currentRunner;
    }

    public static LocalStaticTaskClient instance() {
        return LazyHolder.instance;
    }

    @Override
    public void submit(QueueName name, TaskMessage taskMessage) {
        Preconditions.checkNotNull(currentRunner);
        currentRunner.accept(taskMessage);
    }

    @Override
    public void submit(QueueName name, long countdownMills, TaskMessage taskMessage) {
        Preconditions.checkNotNull(currentRunner);
        Uninterruptibles.sleepUninterruptibly(countdownMills, TimeUnit.MILLISECONDS);
        currentRunner.accept(taskMessage);
    }

    private static class LazyHolder {
        private static final LocalStaticTaskClient instance = new LocalStaticTaskClient();
    }
}
