package jiaoni.daigou.tools;

import jiaoni.common.appengine.access.email.PopupPageEmailClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.httpclient.MockBrowserClient;
import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClientImpl;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.service.appengine.impls.CustomerDbClient;
import jiaoni.daigou.service.appengine.tasks.SyncJiaoniCustomersTaskRunner;

public class VerifySyncJiaoniCustomersTaskRunner {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login(AppEnvs.getHostname(Env.DEV))) {
            SyncJiaoniCustomersTaskRunner runner = new SyncJiaoniCustomersTaskRunner(
                    new CustomerDbClient(
                            Env.DEV,
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