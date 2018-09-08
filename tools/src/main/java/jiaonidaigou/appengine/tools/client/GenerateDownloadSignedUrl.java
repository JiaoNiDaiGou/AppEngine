package jiaonidaigou.appengine.tools.client;

import jiaonidaigou.appengine.tools.remote.ApiClient;

import javax.ws.rs.core.Response;

public class GenerateDownloadSignedUrl {
    public static void main(String[] args) {
        ApiClient client = new ApiClient();

        // Get signed download URL
        Response response = client.newTarget("Get signed download URL")
                .path("/interface/media/signedDownloadUrl")
                .queryParam("fullPath", "my-public-path/unnamed.jpg")
                .request()
                .get();
        System.out.println("\nGet signed download URL");
        String downloadUrl = printAndGetResponse(response);
        System.out.println("signed url: " + downloadUrl);
    }

    private static String printAndGetResponse(final Response response) {
        System.out.println(response.getStatusInfo().getStatusCode() + ":" + response.getStatusInfo().getReasonPhrase());
        String toReturn = response.readEntity(String.class);
        System.out.println(toReturn);
        return toReturn;
    }
}
