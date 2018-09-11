package jiaonidaigou.appengine.tools;

import com.google.common.base.Charsets;
import com.google.common.net.MediaType;
import jiaonidaigou.appengine.api.access.gcp.GoogleCloudLibFactory;
import jiaonidaigou.appengine.api.access.storage.GcsClient;
import jiaonidaigou.appengine.common.utils.Environments;

public class VerifyGcsClient {
    public static void main(String[] args) {
        GcsClient gcsClient = new GcsClient(GoogleCloudLibFactory.storage());

        String content = "this is content";

        String path = Environments.GCS_ROOT_ENDSLASH + "verify_local/abc";

        gcsClient.write(path, MediaType.PLAIN_TEXT_UTF_8.toString(), content.getBytes(Charsets.UTF_8));
    }
}
