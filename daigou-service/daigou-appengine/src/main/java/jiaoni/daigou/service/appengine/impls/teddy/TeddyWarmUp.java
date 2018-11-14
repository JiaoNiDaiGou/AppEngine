package jiaoni.daigou.service.appengine.impls.teddy;

import com.google.appengine.api.memcache.MemcacheService;
import jiaoni.common.appengine.access.taskqueue.PubSubClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.model.Env;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClient;
import jiaoni.daigou.service.appengine.AppEnvs;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static jiaoni.common.appengine.access.taskqueue.PubSubClient.QueueName.PROD_QUEUE;
import static jiaoni.daigou.service.appengine.impls.teddy.TeddyUtils.LAST_CALL_TS_MEMCACHE_KEY;
import static jiaoni.daigou.service.appengine.impls.teddy.TeddyUtils.LAST_WARM_UP_TS_MEMCACHE_KEY;

@Singleton
public class TeddyWarmUp implements Consumer<TaskMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeddyWarmUp.class);

    private static final Duration COLD_TIME = Duration.standardMinutes(30);

    private final MemcacheService memcache;
    private final TeddyClient teddyClient;
    private final PubSubClient pubSubClient;

    @Inject
    public TeddyWarmUp(final MemcacheService memcache,
                       final PubSubClient pubSubClient,
                       @Named(TeddyAdmins.FOR_WARM_UP) final TeddyClient teddyClient) {
        this.memcache = memcache;
        this.pubSubClient = pubSubClient;
        this.teddyClient = teddyClient;
    }

    public void warmUpAsyncIfNeeded() {
        if (AppEnvs.getEnv() != Env.PROD) {
            LOGGER.info("Ignore Teddy warm up since it is not prod: " + AppEnvs.getEnv());
            return;
        }

        DateTime now = DateTime.now();
        DateTime lastWarmup = safeGetTimestamp(LAST_WARM_UP_TS_MEMCACHE_KEY);

        // If not warm up yet, or last warm up is over 30m
        // start warm up.
        if (lastWarmup == null || now.minus(COLD_TIME).isAfter(lastWarmup)) {
            // No call before, start warm up
            LOGGER.info("WarmUp Teddy!");
            pubSubClient.submit(PROD_QUEUE,
                    TaskMessage.newEmptyMessage(TeddyWarmUp.class));
        }
    }

    @Override
    public void accept(TaskMessage taskMessage) {
        warmup();
        DateTime now = DateTime.now();
        memcache.put(LAST_WARM_UP_TS_MEMCACHE_KEY, now.getMillis());

        // Decide next warm up
        DateTime lastCall = safeGetTimestamp(LAST_CALL_TS_MEMCACHE_KEY);
        long nextWarmupCountdownSec = -1;
        long sinceLastCall = now.getMillis() - (lastCall == null ? 0 : lastCall.getMillis());
        int reachCount = taskMessage.getReachCount();
        if (sinceLastCall < 60 * 1000) { // < 1m, we should warm up frequently
            nextWarmupCountdownSec = 30;
        } else if (sinceLastCall < 30 * 60 * 1000) { // < 30m
            nextWarmupCountdownSec = 60;
        } else if (sinceLastCall < 120 * 60 * 1000) { // < 2h
            nextWarmupCountdownSec = 120;
        } else if (reachCount < 5) {
            nextWarmupCountdownSec = 60;
        }
        if (nextWarmupCountdownSec > 0) {
            LOGGER.info("Schedule next Teddy warm up in {} seconds", nextWarmupCountdownSec);
            pubSubClient.submit(PROD_QUEUE,
                    nextWarmupCountdownSec,
                    taskMessage.toBuilder().increaseReachCount().build());
        }
    }

    private void warmup() {
        if (System.currentTimeMillis() % 2 == 0) {
            teddyClient.getCategories();
        } else {
            teddyClient.getOrderPreviews(1);
        }
    }

    private DateTime safeGetTimestamp(final String key) {
        Long timestamp = (Long) memcache.get(key);
        return timestamp == null ? null : new DateTime(timestamp);
    }
}
