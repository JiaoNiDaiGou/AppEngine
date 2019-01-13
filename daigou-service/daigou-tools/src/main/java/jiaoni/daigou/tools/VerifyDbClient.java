package jiaoni.daigou.tools;

import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.service.appengine.impls.db.CustomerDbClient;

public class VerifyDbClient {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            CustomerDbClient dbClient = new CustomerDbClient(
                    Env.PROD,
                    remoteApi.getDatastoreService(),
                    remoteApi.getMemcacheService());

            dbClient.scan().forEach(t -> System.out.println(t.getName() + " " + t.getPhone().getPhone()));
        }
    }
}
