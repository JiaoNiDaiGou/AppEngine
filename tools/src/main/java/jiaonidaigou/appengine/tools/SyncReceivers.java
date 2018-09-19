package jiaonidaigou.appengine.tools;

import jiaonidaigou.appengine.api.access.db.CustomerDbClient;
import jiaonidaigou.appengine.api.access.email.FakePopupEmailSender;
import jiaonidaigou.appengine.api.tasks.SyncJiaoniCustomersTaskRunner;
import jiaonidaigou.appengine.api.tasks.TaskMessage;
import jiaonidaigou.appengine.common.httpclient.MockBrowserClient;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClientImpl;
import jiaonidaigou.appengine.tools.remote.RemoteApi;

public class SyncReceivers {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {

            SyncJiaoniCustomersTaskRunner runner = new SyncJiaoniCustomersTaskRunner(
                    new CustomerDbClient(remoteApi.getDatastoreService(), Environments.SERVICE_NAME_JIAONIDAIGOU),
                    new TeddyClientImpl(TeddyAdmins.JIAONI, new MockBrowserClient("jiaoni")),
                    new FakePopupEmailSender()
            );

            TaskMessage taskMessage = TaskMessage.builder()
                    .withHandler(SyncJiaoniCustomersTaskRunner.class.getSimpleName())
                    .build();

            runner.accept(taskMessage);
        }
    }
}
