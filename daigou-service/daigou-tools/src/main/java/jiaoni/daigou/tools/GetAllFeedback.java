package jiaoni.daigou.tools;

import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.service.appengine.impls.FeedbackDbClient;
import jiaoni.daigou.wiremodel.entity.sys.Feedback;

import java.util.List;

public class GetAllFeedback {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            FeedbackDbClient dbClient = new FeedbackDbClient(
                    Env.PROD,
                    remoteApi.getDatastoreService()
            );
            list(dbClient);
        }
    }

    private static void delete(FeedbackDbClient client) {
        String[] toDelete = {
                "6231337195274240",
                "5694339984392192",
                "5068254280155136"
        };
        client.delete(toDelete);
    }

    private static void list(FeedbackDbClient client) {
        List<Feedback> feedbacks = client.getAllOpenFeedbacks();
        for (Feedback feedback : feedbacks) {
            System.out.println(ObjectMapperProvider.compactToJson(feedback));
        }
    }
}
