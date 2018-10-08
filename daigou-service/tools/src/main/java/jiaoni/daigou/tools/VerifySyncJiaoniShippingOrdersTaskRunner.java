package jiaoni.daigou.tools;

import jiaoni.common.appengine.access.email.PopupPageEmailClient;
import jiaoni.common.appengine.access.sms.ConsoleSmsClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.httpclient.MockBrowserClient;
import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClient;
import jiaoni.daigou.lib.teddy.TeddyClientImpl;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.service.appengine.impls.ShippingOrderDbClient;
import jiaoni.daigou.service.appengine.tasks.SyncJiaoniShippingOrdersTaskRunner;

import java.util.stream.Collectors;

public class VerifySyncJiaoniShippingOrdersTaskRunner {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login(AppEnvs.getHostname(Env.DEV))) {
            TeddyClient teddyClient = new TeddyClientImpl(TeddyAdmins.JIAONI, new MockBrowserClient("jiaoni"));
            ShippingOrderDbClient dbClient = new ShippingOrderDbClient(Env.DEV, remoteApi.getDatastoreService());

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
