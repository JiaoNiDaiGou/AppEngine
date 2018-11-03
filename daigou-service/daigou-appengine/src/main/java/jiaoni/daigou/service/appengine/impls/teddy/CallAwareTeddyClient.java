package jiaoni.daigou.service.appengine.impls.teddy;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.common.collect.Range;
import jiaoni.daigou.lib.teddy.TeddyClient;
import jiaoni.daigou.lib.teddy.model.Order;
import jiaoni.daigou.lib.teddy.model.OrderPreview;
import jiaoni.daigou.lib.teddy.model.Product;
import jiaoni.daigou.lib.teddy.model.Receiver;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

public class CallAwareTeddyClient implements TeddyClient {
    private final TeddyClient client;
    private final MemcacheService memcache;

    public CallAwareTeddyClient(final TeddyClient client, final MemcacheService memcache) {
        this.client = client;
        this.memcache = memcache;
    }

    @Override
    public List<Receiver> getReceivers(int pageNum) {
        List<Receiver> toReturn = client.getReceivers(pageNum);
        onAfter();
        return toReturn;
    }

    @Override
    public List<Receiver> getReceivers(Range<Integer> pageRange) {
        List<Receiver> toReturn = client.getReceivers(pageRange);
        onAfter();
        return toReturn;
    }

    @Override
    public Order getOrderDetails(long orderId, boolean includeShippingInfo) {
        Order order = client.getOrderDetails(orderId, includeShippingInfo);
        onAfter();
        return order;
    }

    @Override
    public Order getOrderDetails(long orderId) {
        Order order = client.getOrderDetails(orderId);
        onAfter();
        return order;
    }

    @Override
    public Order makeOrder(Receiver receiver, List<Product> products, double totalWeight) {
        Order order = client.makeOrder(receiver, products, totalWeight);
        onAfter();
        return order;
    }

    @Override
    public void cancelOrder(long orderId) {
        client.cancelOrder(orderId);
        onAfter();
    }

    @Override
    public Map<Long, OrderPreview> getOrderPreviews(int pageNum) {
        Map<Long, OrderPreview> toReturn = client.getOrderPreviews(pageNum);
        onAfter();
        return toReturn;
    }

    @Override
    public Map<Long, OrderPreview> getOrderPreviews(Range<Integer> pageRange) {
        Map<Long, OrderPreview> toReturn = client.getOrderPreviews(pageRange);
        onAfter();
        return toReturn;
    }

    @Override
    public List<Product.Category> getCategories() {
        List<Product.Category> toReturn = client.getCategories();
        onAfter();
        return toReturn;
    }

    private void onAfter() {
        memcache.put(TeddyUtils.LAST_CALL_TS_MEMCACHE_KEY, DateTime.now().getMillis());
    }
}
