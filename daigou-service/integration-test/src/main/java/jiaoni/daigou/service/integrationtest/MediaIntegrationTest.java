package jiaoni.daigou.service.integrationtest;

import jiaoni.common.model.Env;
import jiaoni.common.test.TestUtils;
import jiaoni.daigou.tools.remote.ApiClient;
import jiaoni.daigou.wiremodel.entity.MediaObject;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jiaoni.daigou.tools.remote.ApiClient.CUSTOM_SECRET_HEADER;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MediaIntegrationTest {
    private final ApiClient client = new ApiClient(Env.DEV);

    @Test
    public void test_directUpload_directDownload() {
        byte[] bytes = TestUtils.readResourceAsBytes("customer_only.jpg");
        // Direct upload
        MediaObject uploadObject = client.newTarget()
                .path("api/media/directUpload")
                .queryParam("ext", "jpg")
                .request()
                .header(CUSTOM_SECRET_HEADER, client.getCustomSecretHeader())
                .post(Entity.entity(bytes, MediaType.APPLICATION_OCTET_STREAM_TYPE))
                .readEntity(MediaObject.class);

        String mediaId = uploadObject.getId();

        byte[] downloadBytes = client.newTarget()
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
        MediaObject uploadObject = client.newTarget()
                .path("api/media/directUpload")
                .queryParam("ext", "jpg")
                .queryParam("hasDownloadUrl", true)
                .request()
                .header(CUSTOM_SECRET_HEADER, client.getCustomSecretHeader())
                .post(Entity.entity(bytes, MediaType.APPLICATION_OCTET_STREAM_TYPE))
                .readEntity(MediaObject.class);

        final String downloadUrl = uploadObject.getSignedDownloadUrl();

        byte[] downloadBytes = client.newTarget("download", downloadUrl)
                .request()
                .get()
                .readEntity(byte[].class);
        assertArrayEquals(bytes, downloadBytes);
    }

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
