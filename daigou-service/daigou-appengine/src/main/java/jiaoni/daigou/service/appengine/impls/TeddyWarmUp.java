package jiaoni.daigou.service.appengine.impls;

import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.memcache.MemcacheService;
import jiaoni.common.utils.Envs;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClient;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class TeddyWarmUp {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeddyWarmUp.class);

    private static final Duration COLD_TIME = Duration.standardHours(1);

    private static final String KEY = Envs.NAMESPACE_SYS + "." + TeddyWarmUp.class.getSimpleName();

    private final MemcacheService memcacheService;
    private final TeddyClient teddyClient;

    @Inject
    public TeddyWarmUp(final MemcacheService memcacheService,
                       @Named(TeddyAdmins.JIAONI) final TeddyClient teddyClient) {
        this.memcacheService = memcacheService;
        this.teddyClient = teddyClient;
    }

    public void warmUpAsyncIfNeeded() {
        Long lastWarmUp = (Long) memcacheService.get(KEY);
        if (lastWarmUp == null || DateTime.now().minus(COLD_TIME).isAfter(lastWarmUp)) {
            LOGGER.info("WarmUp Teddy!");
            ThreadManager.createBackgroundThread(() -> {
                teddyClient.getOrderPreviews(1);
                memcacheService.put(KEY, DateTime.now().getMillis());
            }).start();
        }
    }
}
