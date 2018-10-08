package jiaoni.daigou.service.integrationtest;

import jiaoni.common.model.Env;
import jiaoni.common.test.ApiClient;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.wiremodel.entity.sys.Feedback;
import org.junit.Test;

import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FeedbackIntegrationTest {
    private final ApiClient apiClient = new ApiClient(AppEnvs.getHostname(Env.DEV));

    @Test
    public void test_post_get_close() {
        Feedback feedback = Feedback.newBuilder()
                .setContent("test content")
                .build();

        Feedback afterPost = apiClient.newTarget()
                .path("/api/sys/feedback/post")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(feedback))
                .readEntity(Feedback.class);

        assertNotNull(afterPost.getId());
        assertTrue(afterPost.getOpen());

        List<Feedback> allOpenFeedbacks = apiClient.newTarget()
                .path("/api/sys/feedback/get/allOpen")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<List<Feedback>>() {
                });
        assertTrue(allOpenFeedbacks.contains(afterPost));

        Feedback afterClose = apiClient.newTarget()
                .path("/api/sys/feedback/" + afterPost.getId() + "/close")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json("anything"))
                .readEntity(Feedback.class);
        assertFalse(afterClose.getOpen());

        allOpenFeedbacks = apiClient.newTarget()
                .path("/api/sys/feedback/get/allOpen")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<List<Feedback>>() {
                });
        assertFalse(allOpenFeedbacks.contains(afterPost));
    }
}
