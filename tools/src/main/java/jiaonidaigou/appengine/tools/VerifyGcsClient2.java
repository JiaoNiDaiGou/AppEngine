package jiaonidaigou.appengine.tools;

import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import jiaonidaigou.appengine.wiremodel.entity.MediaObject;

import javax.ws.rs.core.Response;

import static jiaonidaigou.appengine.tools.ResponseHandler.handle;

public class VerifyGcsClient2 {
    public static void main(String[] args) throws Exception {
        ApiClient client = new ApiClient(Environments.MAIN_VERSION_GAE_HOSTNAME);

//        // Get signed upload URL
//        Response response = client.newTarget("Get signed upload URL")
//                .path("/api/media/url/upload")
//                .queryParam("ext", "txt")
//                .request()
//                .get();
//        System.out.println("Get signed upload URL");
//        MediaObject uploadObject = handle(response, MediaObject.class);
//
//        final String mediaId = uploadObject.getId();
//
//        // Upload
//        URL uploadUrl = new URL(uploadObject.getSignedUploadUrl());
//        HttpURLConnection connection = (HttpURLConnection) uploadUrl.openConnection();
//        connection.setDoOutput(true);
//        connection.setRequestMethod("PUT");
//        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
//            outputStream.writeBytes("some content");
//        }
//        handle(connection);
//
//        response = client.newTarget("Directly PUT signed URL", uploadObject.getSignedUploadUrl())
//                .request()
//                .put(Entity.entity("some file", MediaType.APPLICATION_OCTET_STREAM_TYPE));
//        System.out.println("\nPUT to GCS");
//        handle(response);

        // Get signed download URL
        Response response = client.newTarget("Get signed download URL")
                .path("/api/media/url/download")
                .queryParam("media_id", "temp.txt")
                .request()
                .get();
        System.out.println("\nGet signed download URL");
        MediaObject downloadObject = handle(response, MediaObject.class);

        // Download
        response = client.newTarget("Directly GET signed URL", downloadObject.getSignedDownloadUrl())
                .request()
                .get();
        System.out.println("\nGET from GCS");
        handle(response);
    }
}
