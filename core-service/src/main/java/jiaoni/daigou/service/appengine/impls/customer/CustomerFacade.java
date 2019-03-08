package jiaoni.daigou.service.appengine.impls.customer;

import jiaoni.daigou.service.appengine.impls.db.v2.CustomerDbClient;
import jiaoni.daigou.v2.entity.Address;
import jiaoni.daigou.v2.entity.Customer;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Facade to {@link Customer}.
 */
@Singleton
public class CustomerFacade {
    private final CustomerDbClient dbClient;

    @Inject
    public CustomerFacade(final CustomerDbClient dbClient) {
        this.dbClient = dbClient;
    }

    public List<Customer> getAllCustomers() {
        return dbClient.scan().collect(Collectors.toList());
    }

    public Customer getCustomerByNameAndPhone(final String name, final String phone) {
        final String key = CustomerDbClient.computeKey(name, phone);
        return dbClient.getById(key);
    }

    public Customer getCustomerById(final String id) {
        return dbClient.getById(id);
    }

    /**
     * If the customer not exists, create it. Otherwise, update it (NOTE the phone and the name cannot be updated).
     */
    public Customer createOrUpdateCustomer(final Customer customer) {
        checkNotNull(customer);

        String id = CustomerDbClient.computeKey(customer.getName(), customer.getPhone());
        Customer existingCustomer = dbClient.getById(id);
        if (existingCustomer == null) {
            Customer toCreate = customer.toBuilder()
                    .setId(id)
                    .setCreationTime(System.currentTimeMillis())
                    .setLastUpdateTime(System.currentTimeMillis())
                    .build();
            return dbClient.put(toCreate);
        }

        List<Address> newAddresses = customer.getAddressesList()
                .stream()
                .filter(t -> !existingCustomer.getAddressesList().contains(t))
                .collect(Collectors.toList());

        Customer toUpdate = existingCustomer.toBuilder()
                .clearAddresses()
                .addAllAddresses(newAddresses) // The new address will be the default one.
                .addAllAddresses(existingCustomer.getAddressesList())
                .setDefaultAddressIndex(customer.getDefaultAddressIndex())
                .setLastUpdateTime(System.currentTimeMillis())
                .build();
        return dbClient.put(toUpdate);
    }

    /**
     * Put customer into DB. it may overwrite the existing customer.
     */
    @Deprecated
    public Customer putCustomer(final Customer customer) {
        return dbClient.put(customer);
    }
}
