package jiaonidaigou.appengine.tools;

import com.google.common.collect.ImmutableMap;
import jiaonidaigou.appengine.api.access.db.core.InMemoryDbClient;
import jiaonidaigou.appengine.api.access.email.PopupPageEmailClient;
import jiaonidaigou.appengine.api.access.storage.LocalFileStorageClient;
import jiaonidaigou.appengine.api.access.taskqueue.LocalStaticTaskClient;
import jiaonidaigou.appengine.api.registry.Registry;
import jiaonidaigou.appengine.api.tasks.DumpJiaoniShippingOrderTaskRunner;
import jiaonidaigou.appengine.api.tasks.TaskMessage;
import jiaonidaigou.appengine.common.httpclient.MockBrowserClient;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClientImpl;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public class VerifyDumpJiaoniShippingOrderTaskRunner {
    public static void main(String[] args) {
        Registry registry = new Registry(new InMemoryDbClient<>(Pair::getLeft, (t, id) -> t));
        DumpJiaoniShippingOrderTaskRunner runner = new DumpJiaoniShippingOrderTaskRunner(
                new PopupPageEmailClient(),
                new LocalFileStorageClient(),
                LocalStaticTaskClient.instance(),
                new TeddyClientImpl(TeddyAdmins.JIAONI, new MockBrowserClient("jiaoni")),
                registry);
        LocalStaticTaskClient.initialize(runner);

        Map<String, Object> obj = ImmutableMap
                .<String, Object>builder()
                .put("id", 134009L)
                .put("limit", 1)
                .put("backward", false)
                .build();
        runner.accept(TaskMessage.builder()
                .withHandler(DumpJiaoniShippingOrderTaskRunner.class)
                .withPayloadJson(obj)
                .build());
    }
}
