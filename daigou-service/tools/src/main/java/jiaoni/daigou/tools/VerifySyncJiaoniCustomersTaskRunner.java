package jiaoni.daigou.tools;

import jiaoni.common.appengine.access.email.PopupPageEmailClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.httpclient.MockBrowserClient;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClientImpl;
import jiaoni.daigou.service.appengine.impls.CustomerDbClient;
import jiaoni.daigou.service.appengine.tasks.SyncJiaoniCustomersTaskRunner;
import jiaoni.daigou.tools.remote.RemoteApi;

public class VerifySyncJiaoniCustomersTaskRunner {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {

            SyncJiaoniCustomersTaskRunner runner = new SyncJiaoniCustomersTaskRunner(
                    new CustomerDbClient(
                            remoteApi.getDatastoreService(),
                            remoteApi.getMemcacheService()),
                    new TeddyClientImpl(TeddyAdmins.JIAONI, new MockBrowserClient("jiaoni")),
                    new PopupPageEmailClient()
            );

            TaskMessage taskMessage = TaskMessage.builder()
                    .withHandler(SyncJiaoniCustomersTaskRunner.class.getSimpleName())
                    .build();

            runner.accept(taskMessage);
        }
    }
}
