package jiaonidaigou.appengine.lib.teddy;

import com.google.common.collect.Range;
import jiaonidaigou.appengine.lib.teddy.model.Order;
import jiaonidaigou.appengine.lib.teddy.model.OrderPreview;
import jiaonidaigou.appengine.lib.teddy.model.Product;
import jiaonidaigou.appengine.lib.teddy.model.Receiver;

import java.util.List;
import java.util.Map;

public interface TeddyClient {
    /**
     * Load receivers meterOn given page number.
     *
     * @param pageNum Page number. 1 based.
     * @return Receivers by phone.
     */
    Map<String, Receiver> getReceivers(final int pageNum);

    /**
     * Load receivers meterOn given page number ranges.
     *
     * @param pageRange Page number range. 1 based. Must have lower and upper bound.
     * @return Receivers by phone.
     */
    Map<String, Receiver> getReceivers(final Range<Integer> pageRange);

    /**
     * Get order details by id.
     *
     * @param orderId             Order ID.
     * @param includeShippingInfo Whether to include detailed shipping info.
     * @return The order details. NOTE that it doesn't have shipping history.
     */
    Order getOrderDetails(final long orderId, final boolean includeShippingInfo);

    /**
     * Get order details by id. By default, shipping infomation is included.
     *
     * @param orderId Order ID.
     * @return The order details. NOTE that it doesn't have shipping history.
     */
    Order getOrderDetails(final long orderId);

    Order makeOrder(final Receiver receiver,
                    final List<Product> products,
                    final double totalWeight);

    /**
     * Load order previews meterOn given page number.
     *
     * @param pageNum Page number. 1 based.
     * @return Order previews by order ID.
     */
    Map<Long, OrderPreview> getOrderPreviews(final int pageNum);

    /**
     * Load order previews meterOn given page number ranges.
     *
     * @param pageRange Page number range. 1 based. Must have lower and upper bound.
     * @return Order previews by order ID.
     */
    Map<Long, OrderPreview> getOrderPreviews(final Range<Integer> pageRange);
}
