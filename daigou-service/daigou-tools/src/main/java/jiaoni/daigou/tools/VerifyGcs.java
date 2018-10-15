package jiaoni.daigou.tools;

import jiaoni.common.appengine.access.storage.GcsClient;
import jiaoni.common.appengine.access.storage.StorageClient;
import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.service.appengine.AppEnvs;

import java.util.List;

public class VerifyGcs {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login(AppEnvs.getHostname(Env.DEV))) {
            StorageClient storageClient = new GcsClient(remoteApi.getStorage());
            List<String> files = storageClient.listAll("gs://fluid-crane-200921.appspot.com/teddy_orders_dump");
            System.out.println(files);
        }
    }
}
