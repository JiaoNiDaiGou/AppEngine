package jiaoni.daigou.service.appengine.impls.customer;

import jiaoni.daigou.service.appengine.impls.db.v2.CustomerDbClient;
import jiaoni.daigou.v2.entity.Customer;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import static jiaoni.common.utils.Preconditions2.checkNotBlank;

/**
 * Access to {@link Customer}.
 */
@Singleton
public class CustomerAccess {
    private final CustomerDbClient dbClient;

    @Inject
    public CustomerAccess(final CustomerDbClient dbClient) {
        this.dbClient = dbClient;
    }

    @Nullable
    public Customer getCustomerByCustomerId(final String customerId) {
        checkNotBlank(customerId);
        return dbClient.getById(customerId);
    }

    @Nullable
    public Customer getCustomerByNameAndPhone(final String name, final String phone) {
        checkNotBlank(name);
        checkNotBlank(phone);
        return dbClient.getById(CustomerDbClient.computeKey(name, phone));
    }
}
