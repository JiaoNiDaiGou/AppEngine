package jiaoni.daigou.service.appengine.tasks;

import com.google.appengine.api.memcache.MemcacheService;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClient;
import org.joda.time.DateTime;

import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static jiaoni.daigou.service.appengine.utils.TeddyUtils.WARM_UP_MEMCACHE_KEY;

@Singleton
public class TeddyWarmupTaskRunner implements Consumer<TaskMessage> {
    private final MemcacheService memcacheService;
    private final TeddyClient teddyClient;

    @Inject
    public TeddyWarmupTaskRunner(final MemcacheService memcacheService,
                                 @Named(TeddyAdmins.JIAONI) final TeddyClient teddyClient) {
        this.memcacheService = memcacheService;
        this.teddyClient = teddyClient;
    }

    @Override
    public void accept(TaskMessage taskMessage) {
        teddyClient.getOrderPreviews(1);
        memcacheService.put(WARM_UP_MEMCACHE_KEY, DateTime.now().getMillis());
    }
}
