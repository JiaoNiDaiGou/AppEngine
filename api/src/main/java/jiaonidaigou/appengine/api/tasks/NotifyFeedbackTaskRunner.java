package jiaonidaigou.appengine.api.tasks;

import jiaonidaigou.appengine.api.access.db.FeedbackDbClient;
import jiaonidaigou.appengine.api.access.email.EmailClient;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.wiremodel.entity.sys.Feedback;

import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NotifyFeedbackTaskRunner implements Consumer<TaskMessage> {
    private final FeedbackDbClient dbClient;
    private final EmailClient emailClient;

    @Inject
    public NotifyFeedbackTaskRunner(final FeedbackDbClient dbClient,
                                    final EmailClient emailClient) {
        this.dbClient = dbClient;
        this.emailClient = emailClient;
    }

    @Override
    public void accept(TaskMessage taskMessage) {
        List<Feedback> openFeedbacks = dbClient.getAllOpenFeedbacks();
        StringBuilder stringBuilder = new StringBuilder();
        for (Feedback feedback : openFeedbacks) {
            stringBuilder.append("from:")
                    .append(feedback.getRequesterName())
                    .append("\n")
                    .append(feedback.getContent())
                    .append("\n\n");
        }
        String text = stringBuilder.length() == 0 ? "No feedback :(" : stringBuilder.toString();
        for (String email : Environments.ADMIN_EMAILS) {
            emailClient.sendText(email, "Feedback", text);
        }
    }
}
