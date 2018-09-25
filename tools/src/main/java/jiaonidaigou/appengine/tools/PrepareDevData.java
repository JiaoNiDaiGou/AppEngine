package jiaonidaigou.appengine.tools;

import jiaonidaigou.appengine.api.access.db.ShippingOrderDbClient;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.tools.remote.RemoteApi;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;

import java.util.List;
import java.util.stream.Collectors;

import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.CN_POSTMAN_ASSIGNED;
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.CN_TRACKING_NUMBER_ASSIGNED;
import static jiaonidaigou.appengine.wiremodel.entity.ShippingOrder.Status.DELIVERED;

public class PrepareDevData {
    private static ShippingOrder.Status revert(ShippingOrder.Status status) {
        switch (status) {
            case INIT:
                return ShippingOrder.Status.EXTERNAL_SHIPPING_CREATED;
            case PACKED:
                return ShippingOrder.Status.EXTERNAL_SHPPING_PENDING;
            case EXTERNAL_SHIPPING_CREATED:
                return CN_TRACKING_NUMBER_ASSIGNED;
            case EXTERNAL_SHPPING_PENDING:
                return CN_POSTMAN_ASSIGNED;
            case CN_TRACKING_NUMBER_ASSIGNED:
                return DELIVERED;
            default:
                return status;
        }
    }

    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {

//            ShippingOrderDbClient oldClient = new ShippingOrderDbClient(remoteApi.getDatastoreService(), Env.PROD);
//
//            List<ShippingOrder> orders = oldClient.scan()
//                    .map(t -> {
//                        ShippingOrder.Status from = t.getStatus();
//                        ShippingOrder.Status to = revert(from);
//                        return t.toBuilder().setStatus(to).build();
//                    })
//                    .collect(Collectors.toList());
//            oldClient.put(orders);
//
//            System.out.println(orders.size());


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
