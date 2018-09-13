package jiaonidaigou.appengine.tools;

import com.google.common.net.HttpHeaders;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import jiaonidaigou.appengine.wiremodel.entity.MediaObject;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static jiaonidaigou.appengine.tools.ResponseHandler.handle;

public class VerifyGcsClient2 {
    public static void main(String[] args) throws Exception {
        ApiClient client = new ApiClient(Env.PROD);
        Response response;

//        // Get signed upload URL
//        Response response = client.newTarget("Get signed upload URL")
//                .path("/api/media/url/upload")
//                .queryParam("ext", "txt")
//                .request()
//                .header(HttpHeaders.AUTHORIZATION, "Bearer " + client.getGoogleAuthToken())
//                .get();
//        System.out.println("Get signed upload URL");
//        MediaObject uploadObject = handle(response, MediaObject.class);
//
//        final String mediaId = uploadObject.getId();
//
//        // Upload
//        response = client.newTarget("Directly PUT signed URL", uploadObject.getSignedUploadUrl())
//                .request()
//                .put(Entity.entity("some content", uploadObject.getMediaType()));
//        System.out.println("\nPUT to GCS");
//        handle(response);

        response = client.newTarget("Direct upload")
                .path("/api/media/directUpload")
                .queryParam("ext", "txt")
                .queryParam("hasDownloadUrl", true)
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + client.getGoogleAuthToken())
                .post(Entity.entity("this is some content", MediaType.APPLICATION_OCTET_STREAM));
        MediaObject mediaObject = handle(response, MediaObject.class);

//        // Get signed download URL
//        response = client.newTarget("Get signed download URL")
//                .path("/api/media/url/download")
//                .queryParam("media_id", uploadObject.getId())
//                .request()
//                .header(HttpHeaders.AUTHORIZATION, "Bearer " + client.getGoogleAuthToken())
//                .get();
//        System.out.println("\nGet signed download URL");
//        MediaObject downloadObject = handle(response, MediaObject.class);

        // Download
        response = client.newTarget("Directly GET signed URL", mediaObject.getSignedDownloadUrl())
                .request()
                .get();
        System.out.println("\nGET from GCS");
        handle(response);
    }
}
