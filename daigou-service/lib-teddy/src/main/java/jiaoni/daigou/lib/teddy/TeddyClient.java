package jiaoni.daigou.lib.teddy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import jiaoni.daigou.lib.teddy.model.Order;
import jiaoni.daigou.lib.teddy.model.OrderPreview;
import jiaoni.daigou.lib.teddy.model.Product;
import jiaoni.daigou.lib.teddy.model.Receiver;

import java.util.List;
import java.util.Map;

public interface TeddyClient {
    /**
     * This is the known categories.
     * If this change, the price would change. We should NOT make any order until fixing it.
     */
    List<Product.Category> KNOWN_CATEGORIES = ImmutableList.of(Product.Category.BAGS,
            Product.Category.MAKE_UP,
            Product.Category.WATCH_AND_ACCESSORIES,
            Product.Category.LARGE_ITEMS,
            Product.Category.SMALL_APPLIANCES,
            Product.Category.LARGE_COMMERCIAL_GOODS,
            Product.Category.CLOTHES_AND_SHOES,
            Product.Category.HEALTH_SUPPLEMENTS,
            Product.Category.BABY_PRODUCTS,
            Product.Category.FOOD,
            Product.Category.TOYS_AND_DAILY_NECESSITIES,
            Product.Category.MILK_POWDER);

    /**
     * Load receivers on given page number.
     *
     * @param pageNum Page number. 1 based.
     * @return Receivers by phone.
     */
    List<Receiver> getReceivers(final int pageNum);

    /**
     * Load receivers on given page number ranges.
     *
     * @param pageRange Page number range. 1 based. Must have lower and upper bound.
     * @return Receivers by phone.
     */
    List<Receiver> getReceivers(final Range<Integer> pageRange);

    /**
     * Get order details by id.
     *
     * @param orderId             Order ID.
     * @param includeShippingInfo Whether to include detailed shipping info.
     * @return The order details. NOTE that it doesn't have shipping history.
     */
    Order getOrderDetails(final long orderId, final boolean includeShippingInfo);

    /**
     * Get order details by id. By default, shipping information is included.
     *
     * @param orderId Order ID.
     * @return The order details. NOTE that it doesn't have shipping history.
     */
    Order getOrderDetails(final long orderId);

    /**
     * Make a new order.
     */
    Order makeOrder(final Receiver receiver,
                    final List<Product> products,
                    final double totalWeight);

    /**
     * Cancel order.
     *
     * @param orderId Order ID.
     */
    void cancelOrder(final long orderId);

    /**
     * Load order previews on given page number.
     *
     * @param pageNum Page number. 1 based.
     * @return Order previews by order ID.
     */
    Map<Long, OrderPreview> getOrderPreviews(final int pageNum);

    /**
     * Load order previews on given page number ranges.
     *
     * @param pageRange Page number range. 1 based. Must have lower and upper bound.
     * @return Order previews by order ID.
     */
    Map<Long, OrderPreview> getOrderPreviews(final Range<Integer> pageRange);

    /**
     * Load the latest categories.
     */
    List<Product.Category> getCategories();
}
