package jiaonidaigou.appengine.tools;

import jiaonidaigou.appengine.api.access.db.ShippingOrderDbClient;
import jiaonidaigou.appengine.api.access.email.PopupPageEmailClient;
import jiaonidaigou.appengine.api.access.sms.ConsoleSmsClient;
import jiaonidaigou.appengine.api.tasks.SyncJiaoniShippingOrdersTaskRunner;
import jiaonidaigou.appengine.api.tasks.TaskMessage;
import jiaoni.common.httpclient.MockBrowserClient;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClient;
import jiaonidaigou.appengine.lib.teddy.TeddyClientImpl;
import jiaonidaigou.appengine.tools.remote.RemoteApi;

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
