package jiaoni.daigou.tools;

import jiaoni.common.appengine.access.email.PopupPageEmailClient;
import jiaoni.common.appengine.access.gcp.GoogleClientFactory;
import jiaoni.common.appengine.access.storage.GcsClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.service.appengine.tasks.TeddyRankTaskRunner;

public class VerifyTeddyRankTask {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {

            TeddyRankTaskRunner underTest = new TeddyRankTaskRunner(
                    new GcsClient(GoogleClientFactory.storage()),
                    new PopupPageEmailClient());

            underTest.accept(TaskMessage.newEmptyMessage(TeddyRankTaskRunner.class));
        }
    }
}
