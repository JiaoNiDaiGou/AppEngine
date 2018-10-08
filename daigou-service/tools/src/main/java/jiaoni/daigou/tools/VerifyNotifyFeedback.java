package jiaoni.daigou.tools;

import jiaoni.common.appengine.access.email.PopupPageEmailClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.model.Env;
import jiaoni.daigou.service.appengine.impls.FeedbackDbClient;
import jiaoni.daigou.service.appengine.tasks.AdminReportTaskRunner;
import jiaoni.daigou.tools.remote.RemoteApi;

public class VerifyNotifyFeedback {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {

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
