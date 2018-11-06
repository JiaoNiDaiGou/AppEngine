package jiaoni.daigou.service.appengine.impls.customer;

import jiaoni.common.wiremodel.Address;
import jiaoni.common.wiremodel.PhoneNumber;
import jiaoni.daigou.service.appengine.impls.db.CustomerDbClient;
import jiaoni.daigou.wiremodel.entity.Customer;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class CustomerFacade {
    private final CustomerDbClient dbClient;

    @Inject
    public CustomerFacade(final CustomerDbClient dbClient) {
        this.dbClient = dbClient;
    }

    public Customer getCustomer(final PhoneNumber phoneNumber, String name) {
        String key = CustomerDbClient.computeKey(phoneNumber, name);
        return dbClient.getById(key);
    }

    /**
     * If the customer not exists, create it. Otherwise, update it (NOTE the phone and the name cannot be updated).
     */
    public Customer createOrUpdateCustomer(final Customer customer) {
        checkNotNull(customer);

        String key = CustomerDbClient.computeKey(customer.getPhone(), customer.getName());
        Customer existing = dbClient.getById(key);
        if (existing == null) {
            return dbClient.putAndUpdateTimestamp(customer.toBuilder().setId(key).build());
        }

        List<Address> newAddresses = customer.getAddressesList()
                .stream()
                .filter(t -> !existing.getAddressesList().contains(t))
                .collect(Collectors.toList());

        Customer toUpdate = existing.toBuilder()
                .clearAddresses()
                .addAllAddresses(newAddresses) // The new address will be the default one.
                .addAllAddresses(existing.getAddressesList())
                .setDefaultAddressIndex(0)
                .build();

        return dbClient.putAndUpdateTimestamp(toUpdate);
    }

    /**
     * Put customer into DB. it may overwrite the existing customer.
     */
    public Customer putCustomer(final Customer customer) {
        return dbClient.put(customer);
    }
}
