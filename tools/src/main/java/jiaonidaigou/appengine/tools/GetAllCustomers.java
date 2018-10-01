package jiaonidaigou.appengine.tools;

import jiaonidaigou.appengine.api.access.db.CustomerDbClient;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.tools.remote.RemoteApi;
import jiaonidaigou.appengine.wiremodel.entity.Customer;

import java.util.List;
import java.util.stream.Collectors;

public class GetAllCustomers {
    public static void main(String[] args) throws Exception {

        try (RemoteApi remoteApi = RemoteApi.login()) {

            CustomerDbClient client = new CustomerDbClient(
                    remoteApi.getDatastoreService(),
                    Environments.NAMESPACE_JIAONIDAIGOU,
                    Env.DEV
            );
            List<Customer> customers = client.scan()
                    .map(t -> {
                        Customer.Builder builder = t.toBuilder();
                        builder.getPhoneBuilder().setPhone("12345678901");
                        builder.getSocialContactsBuilder().setTeddyUserId("").build();
                        return builder.build();
                    })
                    .collect(Collectors.toList());
            client.put(customers);
        }
    }
}
