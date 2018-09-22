package jiaonidaigou.appengine.tools;

import jiaonidaigou.appengine.api.access.db.ShippingOrderDbClient;
import jiaonidaigou.appengine.api.access.email.PopupPageEmailClient;
import jiaonidaigou.appengine.api.access.sms.ConsoleSmsClient;
import jiaonidaigou.appengine.api.tasks.SyncJiaoniShippingOrdersTaskRunner;
import jiaonidaigou.appengine.api.tasks.TaskMessage;
import jiaonidaigou.appengine.common.httpclient.MockBrowserClient;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClientImpl;
import jiaonidaigou.appengine.tools.remote.RemoteApi;

public class VerifySyncJiaoniShippingOrdersTaskRunner {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            SyncJiaoniShippingOrdersTaskRunner taskRunner = new SyncJiaoniShippingOrdersTaskRunner(
                    new TeddyClientImpl(TeddyAdmins.JIAONI, new MockBrowserClient("jiaoni")),
                    new TeddyClientImpl(TeddyAdmins.HACK, new MockBrowserClient("hack")),
                    new ShippingOrderDbClient(remoteApi.getDatastoreService()),
                    new PopupPageEmailClient(),
                    new ConsoleSmsClient()
            );


            taskRunner.accept(TaskMessage.builder().build());

        }
    }
}
