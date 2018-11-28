package jiaoni.daigou.service.appengine.impls.shippingorders;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jiaoni.common.appengine.access.db.DbQuery;
import jiaoni.common.appengine.access.db.DbSort;
import jiaoni.common.appengine.access.db.PageToken;
import jiaoni.common.appengine.access.storage.StorageClient;
import jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient;
import jiaoni.daigou.wiremodel.entity.ShippingOrder;
import jiaoni.wiremodel.common.entity.PaginatedResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient.FIELD_CREATION_TIME;
import static jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient.FIELD_CUSTOMER_ID;
import static jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient.FIELD_STATUS_NUM;

/**
 * Facade to {@link ShippingOrder}s.
 */
@Singleton
public class ShippingOrderFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingOrderFacade.class);

    private final StorageClient storageClient;
    private final ShippingOrderDbClient shippingOrderDbClient;

    @Inject
    public ShippingOrderFacade(final StorageClient storageClient,
                               final ShippingOrderDbClient shippingOrderDbClient) {
        this.storageClient = storageClient;
        this.shippingOrderDbClient = shippingOrderDbClient;
    }

    public PaginatedResults<ShippingOrder> queryAll(final int limit, final PageToken pageToken) {
        return shippingOrderDbClient.queryInPagination(
                null,
                DbSort.desc(FIELD_CREATION_TIME),
                limit,
                pageToken);
    }

    public PaginatedResults<ShippingOrder> queryByCustomerId(final String customerId, final int limit, final PageToken pageToken) {
        DbQuery dbQuery = DbQuery.eq(FIELD_CUSTOMER_ID, customerId);
        return shippingOrderDbClient.queryInPagination(dbQuery, limit, pageToken);
    }

    public List<ShippingOrder> queryAllNonDelivered() {
        DbQuery query = DbQuery.notEq(FIELD_STATUS_NUM, ShippingOrder.Status.DELIVERED_VALUE);
        return shippingOrderDbClient.queryInStream(query)
                .sorted((a, b) -> Long.compare(b.getCreationTime(), a.getCreationTime()))
                .collect(Collectors.toList());
    }
}
