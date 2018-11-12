package jiaoni.daigou.service.appengine.impls.shippingorders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import jiaoni.common.appengine.access.db.DbQuery;
import jiaoni.common.appengine.access.db.DbSort;
import jiaoni.common.appengine.access.db.PageToken;
import jiaoni.common.appengine.access.storage.StorageClient;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.InternalIOException;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient;
import jiaoni.daigou.wiremodel.entity.ShippingOrder;
import jiaoni.wiremodel.common.entity.PaginatedResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient.FIELD_CREATION_TIME;
import static jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient.FIELD_CUSTOMER_ID;
import static jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient.FIELD_TEDDY_ORDER_ID;

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

    public PaginatedResults<ShippingOrder> queryShippingOrdersByCustomerId(final String customerId, final int limit, final PageToken pageToken) {
        DbQuery dbQuery = DbQuery.eq(FIELD_CUSTOMER_ID, customerId);
        return shippingOrderDbClient.queryInPagination(dbQuery, limit, pageToken);
    }

    public List<ShippingOrder> getAllShippingOrdersByTeddyOrderIdRange(final long minTeddyIdInclusive,
                                                                       final long maxTeddyIdInclusive) {
        DbQuery query = DbQuery.and(
                DbQuery.ge(FIELD_TEDDY_ORDER_ID, minTeddyIdInclusive),
                DbQuery.le(FIELD_TEDDY_ORDER_ID, maxTeddyIdInclusive)
        );
        return shippingOrderDbClient.queryInStream(query)
                .sorted((a, b) -> Long.compare(b.getCreationTime(), a.getCreationTime()))
                .collect(Collectors.toList());
    }

    public List<ShippingOrder> getTeddyAllArchivedShippingOrders() {
        LOGGER.info("Load all archived shipping orders");

        List<String> paths = storageClient.listAll(AppEnvs.Dir.SHIPPING_ORDERS_ARCHIVE);
        if (paths.isEmpty()) {
            return ImmutableList.of();
        }

        List<ShippingOrder> shippingOrders = new ArrayList<>();
        for (String path : paths) {
            LOGGER.info("Load archived shipping from {}", path);
            shippingOrders.addAll(loadShippingOrders(path));
        }

        return shippingOrders;
    }

    private List<ShippingOrder> loadShippingOrders(final String path) {
        byte[] bytes = storageClient.read(path);
        try {
            return ObjectMapperProvider.get().readValue(bytes, new TypeReference<List<ShippingOrder>>() {
            });
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }
}
