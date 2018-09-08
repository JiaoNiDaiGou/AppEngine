package jiaonidaigou.appengine.tools.client;

import jiaonidaigou.appengine.tools.remote.ApiClient;

import java.util.UUID;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * An example calling API to upload files to GCS.
 */
public class UploadAndDownloadFileToGcs {
    public static void main(String[] args) {
        ApiClient client = new ApiClient();
        String filename = UUID.randomUUID().toString();

        // Get signed upload URL
        Response response = client.newTarget("Get signed upload URL")
                .path("/interface/media/signedUploadUrl")
                .queryParam("use", "example")
                .queryParam("filename", filename)
                .request()
                .get();
        System.out.println("Get signed upload URL");
        String uploadUrl = printAndGetResponse(response);

        // Upload
        response = client.newTarget("Directly PUT signed URL", uploadUrl)
                .request()
                .put(Entity.entity("some file", MediaType.APPLICATION_OCTET_STREAM_TYPE));
        System.out.println("\nPUT to GCS");
        printAndGetResponse(response);

        // Get signed download URL
        response = client.newTarget("Get signed download URL")
                .path("/api/media/signedDownloadUrl")
                .queryParam("path", "example/" + filename)
                .request()
                .get();
        System.out.println("\nGet signed download URL");
        String downloadUrl = printAndGetResponse(response);

        // Download
        response = client.newTarget("Directly GET signed URL", downloadUrl)
                .request()
                .get();
        System.out.println("\nGET from GCS");
        printAndGetResponse(response);
    }

    private static String printAndGetResponse(final Response response) {
        System.out.println(response.getStatusInfo().getStatusCode() + ":" + response.getStatusInfo().getReasonPhrase());
        String toReturn = response.readEntity(String.class);
        System.out.println(toReturn);
        return toReturn;
    }
}
