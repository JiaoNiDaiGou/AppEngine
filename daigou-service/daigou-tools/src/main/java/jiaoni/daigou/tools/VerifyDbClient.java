package jiaoni.daigou.tools;

import jiaoni.common.appengine.access.db.DbClient;
import jiaoni.common.model.Env;
import jiaoni.common.test.ApiClient;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.service.appengine.impls.db.CustomerDbClient;
import jiaoni.daigou.wiremodel.entity.Customer;

import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.GenericType;

import static jiaoni.common.test.ApiClient.CUSTOM_SECRET_HEADER;

/**
 * Verify {@link DbClient}.
 */
public class VerifyDbClient {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            CustomerDbClient dbClient = new CustomerDbClient(
                    Env.DEV,
                    remoteApi.getDatastoreService(),
                    remoteApi.getMemcacheService());

            List<Customer> customers = dbClient.scan().collect(Collectors.toList());
            System.out.println(customers.size());
        }

        ApiClient apiClient = new ApiClient(AppEnvs.getHostname(Env.DEV));

        List<Customer> customers = apiClient.newTarget()
                .path("api/customers/all")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get()
                .readEntity(new GenericType<List<Customer>>() {
                });
        System.out.println(customers.size());
    }



}

