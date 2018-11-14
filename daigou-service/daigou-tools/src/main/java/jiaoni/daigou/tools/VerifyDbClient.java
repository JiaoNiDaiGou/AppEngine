package jiaoni.daigou.tools;

import jiaoni.common.appengine.access.db.DbClient;
import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.service.appengine.impls.db.CustomerDbClient;
import jiaoni.daigou.wiremodel.entity.Customer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

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

            List<Customer> toDelete = new ArrayList<>();
            for (Customer customer : customers) {
                if (isBlank(customer.getName())) {
                    toDelete.add(customer);
                } else if (isBlank(customer.getPhone().getPhone()) ||
                        customer.getAddressesCount() == 0) {
                    toDelete.add(customer);
                }
            }
            dbClient.deleteItems(toDelete);
        }
    }
}
