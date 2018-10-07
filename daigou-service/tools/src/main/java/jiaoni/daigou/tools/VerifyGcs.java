package jiaoni.daigou.tools;

import jiaoni.common.appengine.access.storage.GcsClient;
import jiaoni.common.appengine.access.storage.StorageClient;
import jiaoni.daigou.tools.remote.RemoteApi;

import java.util.List;

public class VerifyGcs {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            StorageClient storageClient = new GcsClient(remoteApi.getStorage());
            List<String> files = storageClient.listAll("gs://fluid-crane-200921.appspot.com/teddy_orders_dump");
            System.out.println(files);
        }
    }
}
