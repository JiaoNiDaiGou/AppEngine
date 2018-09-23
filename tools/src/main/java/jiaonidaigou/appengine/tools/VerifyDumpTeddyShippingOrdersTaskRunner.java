package jiaonidaigou.appengine.tools;

import com.google.common.collect.ImmutableMap;
import jiaonidaigou.appengine.api.access.email.PopupPageEmailClient;
import jiaonidaigou.appengine.api.access.storage.LocalFileStorageClient;
import jiaonidaigou.appengine.api.access.taskqueue.LocalStaticTaskClient;
import jiaonidaigou.appengine.api.tasks.DumpTeddyShippingOrdersTaskRunner;
import jiaonidaigou.appengine.api.tasks.TaskMessage;
import jiaonidaigou.appengine.common.httpclient.MockBrowserClient;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClientImpl;

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
