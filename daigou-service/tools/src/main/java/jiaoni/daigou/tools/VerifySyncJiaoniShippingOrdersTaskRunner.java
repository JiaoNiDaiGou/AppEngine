package jiaoni.daigou.tools;

import jiaoni.common.appengine.access.email.PopupPageEmailClient;
import jiaoni.common.appengine.access.sms.ConsoleSmsClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.httpclient.MockBrowserClient;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClient;
import jiaoni.daigou.lib.teddy.TeddyClientImpl;
import jiaoni.daigou.service.appengine.impls.ShippingOrderDbClient;
import jiaoni.daigou.service.appengine.tasks.SyncJiaoniShippingOrdersTaskRunner;
import jiaoni.daigou.tools.remote.RemoteApi;

import java.util.stream.Collectors;

public class VerifySyncJiaoniShippingOrdersTaskRunner {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            TeddyClient teddyClient = new TeddyClientImpl(TeddyAdmins.JIAONI, new MockBrowserClient("jiaoni"));
            ShippingOrderDbClient dbClient = new ShippingOrderDbClient(remoteApi.getDatastoreService());

            dbClient.deleteItems(dbClient.scan().collect(Collectors.toList()));

            SyncJiaoniShippingOrdersTaskRunner taskRunner = new SyncJiaoniShippingOrdersTaskRunner(
                    teddyClient,
                    new TeddyClientImpl(TeddyAdmins.HACK, new MockBrowserClient("hack")),
                    dbClient,
                    new PopupPageEmailClient(),
                    new ConsoleSmsClient()
            );
            taskRunner.accept(TaskMessage.builder().build());
        }
    }
}
