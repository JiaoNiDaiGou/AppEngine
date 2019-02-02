package jiaoni.daigou.tools;

import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.common.wiremodel.Address;
import jiaoni.daigou.service.appengine.impls.db.CustomerDbClient;
import jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient;
import jiaoni.daigou.wiremodel.entity.Customer;
import jiaoni.daigou.wiremodel.entity.ShippingOrder;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VerifyDbClient {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            CustomerDbClient v1client = new CustomerDbClient(Env.DEV, remoteApi.getDatastoreService(), remoteApi.getMemcacheService());
            List<Customer> customers = v1client.scan().collect(Collectors.toList());
            System.out.println(customers.size());
            customers.forEach(t ->  System.out.println(t.getName() + " " + t.getPhone().getPhone()));
        }
    }

    public static void main2() throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            jiaoni.daigou.service.appengine.impls.db.v2.CustomerDbClient v2Client =
                    new jiaoni.daigou.service.appengine.impls.db.v2.CustomerDbClient(Env.PROD, remoteApi.getDatastoreService(), remoteApi.getMemcacheService());

//            List<Customer> customers = v1Client.scan().collect(Collectors.toList());
//
//            System.out.println(customers.size());
//            List<jiaoni.daigou.v2.entity.Customer> v2Customers = customers.stream().map(t -> toV2Customer(t)).collect(Collectors.toList());
//
//            v1Client.deleteItems(customers);
//            v2Client.put(v2Customers);
//            System.out.println(v2Client.scan().count());

            ShippingOrderDbClient shippingOrderDbClient = new ShippingOrderDbClient(Env.PROD, remoteApi.getDatastoreService());
            List<ShippingOrder> shippingOrders = shippingOrderDbClient.scan().collect(Collectors.toList());
            Map<String, jiaoni.daigou.v2.entity.Customer> v2Customers = new HashMap<>();
            for (ShippingOrder shippingOrder : shippingOrders) {
                Customer customer = shippingOrder.getReceiver();
                if (StringUtils.isAnyBlank(customer.getName(), customer.getPhone().getPhone())) {
                    System.out.println(customer);
                    continue;
                }
                String id = jiaoni.daigou.service.appengine.impls.db.v2.CustomerDbClient.computeKey(customer.getName(), customer.getPhone().getPhone());

                jiaoni.daigou.v2.entity.Customer found = v2Customers.get(id);
                if (found == null) {
                    found = toV2Customer(customer);
                } else {
                    List<jiaoni.daigou.v2.entity.Address> v2Addresses = customer.getAddressesList().stream().map(t -> toV2Address(t)).collect(Collectors.toList());
                    jiaoni.daigou.v2.entity.Customer.Builder foundBuilder = found.toBuilder();
                    for (jiaoni.daigou.v2.entity.Address v2address : v2Addresses) {
                        if (!foundBuilder.getAddressesList().contains(v2address)) {
                            foundBuilder.addAddresses(v2address);
                        }
                    }
                }
                v2Customers.put(customer.getId(), found);
            }

            System.out.println(v2Customers.size());
        }
    }

    private static jiaoni.daigou.v2.entity.Customer toV2Customer(Customer customer) {
        return jiaoni.daigou.v2.entity.Customer.newBuilder()
                .setId(jiaoni.daigou.service.appengine.impls.db.v2.CustomerDbClient.computeKey(customer.getName(), customer.getPhone().getPhone()))
                .setName(customer.getName())
                .setPhone(customer.getPhone().getPhone())
                .addAllAddresses(customer.getAddressesList().stream().map(t -> toV2Address(t)).collect(Collectors.toList()))
                .setIdCard(customer.getIdCard())
                .setDefaultAddressIndex(customer.getDefaultAddressIndex())
                .setCreationTime(System.currentTimeMillis())
                .build();
    }

    private static jiaoni.daigou.v2.entity.Address toV2Address(Address address) {
        return jiaoni.daigou.v2.entity.Address.newBuilder()
                .setCountryCode(address.getCountryCode())
                .setRegion(address.getRegion())
                .setCity(address.getCity())
                .setZone(address.getZone())
                .setAddress(address.getAddress())
                .setPostalCode(address.getPostalCode())
                .build();
    }
}
