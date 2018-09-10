package jiaonidaigou.appengine.tools;

import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import jiaonidaigou.appengine.wiremodel.entity.MediaObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.ws.rs.core.Response;

import static jiaonidaigou.appengine.tools.ResponseHandler.handle;

public class VerifyParseCustomerByImage {
    public static void main(String[] args) throws Exception {
        ApiClient client = new ApiClient(Environments.MAIN_VERSION_GAE_HOSTNAME);

        // Get signed upload URL
        Response response = client.newTarget("Get signed upload URL")
                .path("/api/media/url/upload")
                .queryParam("ext", "jpg")
                .request()
                .get();
        System.out.println("Get signed upload URL");
        MediaObject uploadObject = handle(response, MediaObject.class);

        String mediaId = uploadObject.getId();

        // Upload image
        byte[] bytes;
        try (InputStream inputStream = Resources
                .getResource("customer_only.jpg").openStream()) {
            bytes = ByteStreams.toByteArray(inputStream);
        }

        URL uploadUrl = new URL(uploadObject.getSignedUploadUrl());
        HttpURLConnection connection = (HttpURLConnection) uploadUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
            outputStream.write(bytes);
        }
        handle(connection);
    }
}
