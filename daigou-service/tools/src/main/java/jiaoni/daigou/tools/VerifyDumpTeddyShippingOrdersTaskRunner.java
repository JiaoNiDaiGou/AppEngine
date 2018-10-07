package jiaoni.daigou.tools;

import com.google.common.collect.ImmutableMap;
import jiaoni.common.appengine.access.email.PopupPageEmailClient;
import jiaoni.common.appengine.access.storage.LocalFileStorageClient;
import jiaoni.common.appengine.access.taskqueue.LocalStaticTaskClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.httpclient.MockBrowserClient;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClientImpl;
import jiaoni.daigou.service.appengine.tasks.DumpTeddyShippingOrdersTaskRunner;

import java.util.Map;

public class VerifyDumpTeddyShippingOrdersTaskRunner {
    public static void main(String[] args) {
        DumpTeddyShippingOrdersTaskRunner runner = new DumpTeddyShippingOrdersTaskRunner(
                new PopupPageEmailClient(),
                new LocalFileStorageClient(),
                LocalStaticTaskClient.instance(),
                new TeddyClientImpl(TeddyAdmins.HACK, new MockBrowserClient("jiaoni")));
        LocalStaticTaskClient.initialize(runner);

        Map<String, Object> obj = ImmutableMap
                .<String, Object>builder()
                .put("id", 134154L)
                .put("limit", 1)
                .put("backward", false)
                .build();
        runner.accept(TaskMessage.builder()
                .withHandler(DumpTeddyShippingOrdersTaskRunner.class)
                .withPayloadJson(obj)
                .build());
    }
}
