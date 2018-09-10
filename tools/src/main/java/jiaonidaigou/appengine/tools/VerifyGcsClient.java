package jiaonidaigou.appengine.tools;

import com.google.common.io.CharStreams;
import jiaonidaigou.appengine.api.access.storage.GcsClient;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.tools.remote.RemoteApi;

import java.io.Reader;
import java.io.Writer;

import javax.ws.rs.core.MediaType;

import static com.google.common.base.Preconditions.checkState;

public class VerifyGcsClient {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            String content = "this is a content";

//            GcsClient client = new GcsClient(
//                    remoteApi.getAppIdentityService(),
//                    remoteApi.getGcsService()
//            );
//
//            // Write to GCS
//            String path = Environments.GCS_ROOT_ENDSLASH + "test/VerifyGcsClient.text";
//            try (Writer writer = client.write(path, MediaType.APPLICATION_OCTET_STREAM_TYPE)) {
//                writer.write(content);
//            }
//
//            // Get from GCS
//            String readContent;
//            try (Reader reader = client.read(path)) {
//                readContent = CharStreams.toString(reader);
//            }
//            checkState(content.equals(readContent));
//            System.out.println(readContent);
        }
    }
}
