package jiaonidaigou.appengine.tools;

import jiaonidaigou.appengine.api.access.storage.GcsClient;
import jiaonidaigou.appengine.api.access.storage.StorageClient;
import jiaonidaigou.appengine.tools.remote.RemoteApi;

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
