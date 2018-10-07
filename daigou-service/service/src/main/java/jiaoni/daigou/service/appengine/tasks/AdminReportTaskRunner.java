package jiaoni.daigou.service.appengine.tasks;

import jiaoni.common.appengine.access.email.EmailClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.daigou.wiremodel.entity.sys.Feedback;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.service.appengine.impls.FeedbackDbClient;

import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AdminReportTaskRunner implements Consumer<TaskMessage> {
    private final FeedbackDbClient dbClient;
    private final EmailClient emailClient;

    @Inject
    public AdminReportTaskRunner(final FeedbackDbClient dbClient,
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
        for (String email : AppEnvs.getAdminEmails()) {
            emailClient.sendText(email, "Feedback", text);
        }
    }
}
