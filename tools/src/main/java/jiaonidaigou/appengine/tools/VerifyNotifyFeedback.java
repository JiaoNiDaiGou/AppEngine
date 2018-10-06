package jiaonidaigou.appengine.tools;

import jiaonidaigou.appengine.api.access.db.FeedbackDbClient;
import jiaonidaigou.appengine.api.access.email.PopupPageEmailClient;
import jiaonidaigou.appengine.api.tasks.AdminReportTaskRunner;
import jiaonidaigou.appengine.api.tasks.TaskMessage;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.tools.remote.RemoteApi;

public class VerifyNotifyFeedback {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {

            AdminReportTaskRunner runner = new AdminReportTaskRunner(
                    new FeedbackDbClient(remoteApi.getDatastoreService(), Env.DEV),
                    new PopupPageEmailClient()
            );

            TaskMessage taskMessage = TaskMessage.builder()
                    .withHandler(AdminReportTaskRunner.class)
                    .build();

            runner.accept(taskMessage);
        }
    }
}
