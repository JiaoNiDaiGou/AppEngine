package jiaoni.daigou.tools;

import jiaoni.common.appengine.access.email.PopupPageEmailClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.service.appengine.impls.FeedbackDbClient;
import jiaoni.daigou.service.appengine.tasks.AdminReportTaskRunner;

public class VerifyNotifyFeedback {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login(AppEnvs.getHostname(Env.DEV))) {
            AdminReportTaskRunner runner = new AdminReportTaskRunner(
                    new FeedbackDbClient(Env.DEV, remoteApi.getDatastoreService()),
                    new PopupPageEmailClient()
            );

            TaskMessage taskMessage = TaskMessage.builder()
                    .withHandler(AdminReportTaskRunner.class)
                    .build();

            runner.accept(taskMessage);
        }
    }
}
