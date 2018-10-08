package jiaoni.daigou.service.integrationtest;

import jiaoni.common.model.Env;
import jiaoni.common.test.ApiClient;
import jiaoni.common.test.TestUtils;
import jiaoni.common.wiremodel.MediaObject;
import jiaoni.daigou.service.appengine.AppEnvs;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jiaoni.common.test.ApiClient.CUSTOM_SECRET_HEADER;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MediaIntegrationTest {
    private final ApiClient apiClient = new ApiClient(AppEnvs.getHostname(Env.DEV));

    @Test
    public void test_directUpload_directDownload() {
        byte[] bytes = TestUtils.readResourceAsBytes("customer_only.jpg");
        // Direct upload
        MediaObject uploadObject = apiClient.newTarget()
                .path("api/media/directUpload")
                .queryParam("ext", "jpg")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.entity(bytes, MediaType.APPLICATION_OCTET_STREAM_TYPE))
                .readEntity(MediaObject.class);

        String mediaId = uploadObject.getId();

        byte[] downloadBytes = apiClient.newTarget()
                .path("api/media/directDownload")
                .queryParam("mediaId", mediaId)
                .request()
                // Try no auth
//                .header(CUSTOM_SECRET_HEADER, client.getCustomSecretHeader())
                .get()
                .readEntity(byte[].class);
        assertArrayEquals(bytes, downloadBytes);
    }

    @Test
    public void test_directUpload_download() {
        byte[] bytes = TestUtils.readResourceAsBytes("customer_only.jpg");
        // Direct upload
        MediaObject uploadObject = apiClient.newTarget()
                .path("api/media/directUpload")
                .queryParam("ext", "jpg")
                .queryParam("hasDownloadUrl", true)
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.entity(bytes, MediaType.APPLICATION_OCTET_STREAM_TYPE))
                .readEntity(MediaObject.class);

        final String downloadUrl = uploadObject.getSignedDownloadUrl();

        byte[] downloadBytes = apiClient.newTarget("download", downloadUrl)
                .request()
                .get()
                .readEntity(byte[].class);
        assertArrayEquals(bytes, downloadBytes);
    }

    @Test
    public void test_upload_download() {
        // Get signed upload URL
        MediaObject uploadObject = apiClient.newTarget()
                .path("/api/media/url/upload")
                .queryParam("ext", "txt")
                .request()
                .header(AUTHORIZATION, apiClient.getGoogleAuthTokenBearerHeader())
                .get()
                .readEntity(MediaObject.class);

        final String mediaId = uploadObject.getId();
        assertNotNull(mediaId);

        // Upload
        String content = "this is some content";
        Response response = apiClient.newTarget("upload", uploadObject.getSignedUploadUrl())
                .request()
                .header(AUTHORIZATION, apiClient.getGoogleAuthTokenBearerHeader())
                .put(Entity.text(content));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Get signed download URL
        MediaObject downloadObject = apiClient.newTarget()
                .path("/api/media/url/download")
                .queryParam("mediaId", mediaId)
                .request()
                .header(AUTHORIZATION, apiClient.getGoogleAuthTokenBearerHeader())
                .get()
                .readEntity(MediaObject.class);

        // Download
        String fetchedContent = apiClient.newTarget("download", downloadObject.getSignedDownloadUrl())
                .request()
                .get()
                .readEntity(String.class);
        assertEquals(content, fetchedContent);
    }
}
