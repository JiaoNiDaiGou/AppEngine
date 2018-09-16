package jiaonidaigou.appengine.integrationtest;

import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import jiaonidaigou.appengine.wiremodel.entity.MediaObject;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MediaIntegrationTest {
    private final ApiClient client = new ApiClient(Env.PROD);

    @Test
    public void test_upload_download() {
        // Get signed upload URL
        MediaObject uploadObject = client.newTarget()
                .path("/api/media/url/upload")
                .queryParam("ext", "txt")
                .request()
                .header(AUTHORIZATION, client.getGoogleAuthTokenBearerHeader())
                .get()
                .readEntity(MediaObject.class);

        final String mediaId = uploadObject.getId();
        assertNotNull(mediaId);

        // Upload
        String content = "this is some content";
        Response response = client.newTarget("upload", uploadObject.getSignedUploadUrl())
                .request()
                .header(AUTHORIZATION, client.getGoogleAuthTokenBearerHeader())
                .put(Entity.text(content));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Get signed download URL
        MediaObject downloadObject = client.newTarget()
                .path("/api/media/url/download")
                .queryParam("mediaId", mediaId)
                .request()
                .header(AUTHORIZATION, client.getGoogleAuthTokenBearerHeader())
                .get()
                .readEntity(MediaObject.class);

        // Download
        String fetchedContent = client.newTarget("download", downloadObject.getSignedDownloadUrl())
                .request()
                .get()
                .readEntity(String.class);
        assertEquals(content, fetchedContent);
    }
}
