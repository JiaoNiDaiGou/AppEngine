package jiaoni.daigou.service.appengine.impls;

import com.google.appengine.api.memcache.MemcacheService;
import jiaoni.common.appengine.access.taskqueue.PubSubClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.appengine.access.taskqueue.TaskQueueClient;
import jiaoni.common.model.Env;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.service.appengine.tasks.TeddyWarmupTaskRunner;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static jiaoni.daigou.service.appengine.utils.TeddyUtils.WARM_UP_MEMCACHE_KEY;

@Singleton
public class TeddyWarmUp {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeddyWarmUp.class);

    private static final Duration COLD_TIME = Duration.standardMinutes(30);

    private final MemcacheService memcacheService;
    private final TaskQueueClient taskQueueClient;

    @Inject
    public TeddyWarmUp(final MemcacheService memcacheService,
                       final TaskQueueClient taskQueueClient) {
        this.memcacheService = memcacheService;
        this.taskQueueClient = taskQueueClient;
    }

    public void warmUpAsyncIfNeeded() {
        if (AppEnvs.getEnv() != Env.PROD) {
            LOGGER.info("Ignore Teddy warm up since it is not prod: " + AppEnvs.getEnv());
            return;
        }

        Long lastWarmUp = (Long) memcacheService.get(WARM_UP_MEMCACHE_KEY);
        if (lastWarmUp == null || DateTime.now().minus(COLD_TIME).isAfter(lastWarmUp)) {
            LOGGER.info("WarmUp Teddy!");
            taskQueueClient.submit(PubSubClient.QueueName.HIGH_FREQUENCY,
                    TaskMessage.newEmptyMessage(TeddyWarmupTaskRunner.class));
        }
    }
}
