package jiaonidaigou.appengine.tools;

import jiaonidaigou.appengine.api.access.db.CustomerDbClient;
import jiaonidaigou.appengine.api.access.db.ShippingOrderDbClient;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.tools.remote.RemoteApi;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;

import java.util.List;
import java.util.stream.Collectors;

public class PrepareDevData {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
//            CustomerDbClient customerDbClient = new CustomerDbClient(remoteApi.getDatastoreService(), Environments.NAMESPACE_JIAONIDAIGOU, Env.PROD);
//            List<Customer> customers = customerDbClient.scan()
//                    .map(t -> t.toBuilder().setLastUpdatedTime(System.currentTimeMillis()).build())
//                    .collect(Collectors.toList());
//            customerDbClient.put(customers);

//            ShippingOrderDbClient oldClient = new ShippingOrderDbClient(remoteApi.getDatastoreService(), Env.DEV);
//            List<ShippingOrder> shippingOrders = oldClient.scan()
//                    .map(t -> {
//                        Customer customer = customers.stream()
//                                .filter(c -> c.getName().equals(t.getReceiver().getName()) && c.getPhone().equals(t.getReceiver().getPhone()))
//                                .findFirst().orElse(null);
//                        if (customer == null) {
//                            return t;
//                        }
//                        ShippingOrder.Builder builder = t.toBuilder();
//                        builder.getReceiverBuilder().setId(customer.getId());
//                        return builder.build();
//                    })
//                    .collect(Collectors.toList());
//            System.out.println("old:" + shippingOrders.size());
//
//            oldClient.put(shippingOrders);
//            System.out.println("old:" + oldClient.scan().count());
        }
    }
}
