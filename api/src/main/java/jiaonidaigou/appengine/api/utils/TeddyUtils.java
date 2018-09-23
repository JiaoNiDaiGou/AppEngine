package jiaonidaigou.appengine.api.utils;

import com.google.common.collect.ImmutableSet;
import jiaonidaigou.appengine.lib.teddy.model.Order;
import jiaonidaigou.appengine.lib.teddy.model.OrderPreview;
import jiaonidaigou.appengine.lib.teddy.model.Receiver;
import jiaonidaigou.appengine.lib.teddy.model.ShippingHistoryEntry;
import jiaonidaigou.appengine.wiremodel.entity.Address;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.PhoneNumber;
import jiaonidaigou.appengine.wiremodel.entity.Postman;
import jiaonidaigou.appengine.wiremodel.entity.Price;
import jiaonidaigou.appengine.wiremodel.entity.Product;
import jiaonidaigou.appengine.wiremodel.entity.ProductCategory;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TeddyUtils {
    private static Set<String> KNOWN_SHIPPING_CARRIERS = ImmutableSet.of("邮政平邮", "圆通速递", "邮政包裹");

    public static Set<String> getKnownShippingCarriers() {
        return KNOWN_SHIPPING_CARRIERS;
    }

    public static ShippingOrder convertFromOrderPreview(final OrderPreview preview) {
        ShippingOrder.Builder builder = ShippingOrder.newBuilder()
                .setStatus(ShippingOrder.Status.PENDING);
        setIfNotBlank(String.valueOf(preview.getId()), builder::setTeddyOrderId);
        setIfNotBlank(preview.getFormattedId(), builder::setTeddyFormattedId);
        setIfNotBlank(preview.getReceiverName(), t -> builder.getReceiverBuilder().setName(t));
        setIfNotBlank(preview.getProductSummary(), builder::setProductSummary);
        if (preview.getPrice() > 0) {
            builder.setTotalPrice(Price.newBuilder().setUnit(Price.Unit.USD).setValue(preview.getPrice()));
        }
        String rawStatus = preview.getRawShippingStatus();
        if ("运单创建成功".equals(rawStatus)) {
            builder.setStatus(ShippingOrder.Status.CREATED);
        } else if (rawStatus.contains("(") && rawStatus.contains(")")) {
            String shippingCarrier = StringUtils.substringBefore(rawStatus, "(").trim();
            builder.setShippingCarrier(shippingCarrier);
            String trackingNumber = StringUtils.substringBetween(rawStatus, "(", ")");
            builder.setTrackingNumber(trackingNumber)
                    .setStatus(ShippingOrder.Status.CN_TRACKING_NUMBER_ASSIGNED);
        }
        return builder.build();
    }

    public static ShippingOrder convertShippingOrder(final Order order) {
        ShippingOrder.Builder builder = ShippingOrder.newBuilder()
                .setStatus(convertShippingOrderStatus(order.getStatus()))
                .setReceiver(convertCustomer(order.getReceiver()))
                .setCreationTime(order.getCreationTime().getMillis());
        setIfNotBlank(order.getSenderName(), builder::setSenderName);
        setIfNotNull(convertPostman(order), builder::setPostman);
        setIfNotBlank(order.getTrackingNumber(), builder::setTrackingNumber);
        setIfNotBlank(order.getRawShippingStatus(), builder::setShippingCarrier);
        setIfNotNull(convertShippingEnding(order.getDeliveryEnding()), builder::setShippingEnding);
        return builder.addAllProductEntries(
                order.getProducts()
                        .stream()
                        .map(TeddyUtils::convertShippingOrderProductEntry)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .setProductSummary(order.getProductSummary())
                .addAllShippingHistory(
                        order.getShippingHistory()
                                .stream()
                                .map(TeddyUtils::convertShippingOrderShippingHistory)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()))
                .setTeddyOrderId(String.valueOf(order.getId()))
                .setTeddyFormattedId(order.getFormattedId())
                .build();
    }

    private static ShippingOrder.ShippingHistoryEntry convertShippingOrderShippingHistory(final ShippingHistoryEntry entry) {
        return ShippingOrder.ShippingHistoryEntry.newBuilder()
                .setStatus(entry.getStatus())
                .setTimestamp(entry.getTimestamp().getMillis())
                .build();
    }

    private static ShippingOrder.ProductEntry convertShippingOrderProductEntry(final jiaonidaigou.appengine.lib.teddy.model.Product product) {
        Product.Builder productBuilder = Product.newBuilder();
        boolean hasProduct = setIfNotBlank(product.getBrand(), productBuilder::setBrand)
                || setIfNotNull(convertProductCategory(product.getCategory()), productBuilder::setCategory)
                || setIfNotBlank(product.getName(), productBuilder::setName);
        if (!hasProduct) {
            return null;
        }
        return ShippingOrder.ProductEntry.newBuilder()
                .setProduct(productBuilder)
                .setQuantity(product.getQuantity())
                .setSellPrice(Price.newBuilder().setUnit(Price.Unit.USD).setValue(product.getUnitPriceInDollers()))
                .build();
    }

    private static Customer convertCustomer(final Receiver receiver) {
        if (receiver == null) {
            return null;
        }
        Customer.Builder builder = Customer.newBuilder();
        setIfNotBlank(receiver.getName(), builder::setName);
        setIfNotBlank(receiver.getPhone(), t -> builder.setPhone(PhoneNumber.newBuilder().setCountryCode("86").setPhone(t)));
        Address.Builder addressBuilder = Address.newBuilder();
        boolean hasAddress = setIfNotBlank(receiver.getAddressRegion(), addressBuilder::setRegion)
                || setIfNotBlank(receiver.getAddressCity(), addressBuilder::setCity)
                || setIfNotBlank(receiver.getAddressZone(), addressBuilder::setZone)
                || setIfNotBlank(receiver.getAddress(), addressBuilder::setAddress);
        if (hasAddress) {
            builder.addAddresses(addressBuilder);
        }
        setIfNotBlank(receiver.getUserId(), t -> builder.getSocialContactsBuilder().setTeddyUserId(t));
        return builder.build();
    }

    private static Postman convertPostman(final Order order) {
        if (isNotBlank(order.getPostmanName()) && isNotBlank(order.getPostmanPhone())) {
            return Postman.newBuilder()
                    .setName(order.getPostmanName())
                    .setPhone(PhoneNumber.newBuilder().setCountryCode("86").setPhone(order.getPostmanPhone()).build())
                    .build();
        }
        return null;
    }

    private static ShippingOrder.Status convertShippingOrderStatus(final Order.Status status) {
        if (status == null) {
            return ShippingOrder.Status.DELIVERED;
        }
        switch (status) {
            case CREATED:
                return ShippingOrder.Status.CREATED;
            case PENDING:
                return ShippingOrder.Status.PENDING;
            case POSTMAN_ASSIGNED:
                return ShippingOrder.Status.CN_TRACKING_NUMBER_ASSIGNED;
            case TRACKING_NUMBER_ASSIGNED:
                return ShippingOrder.Status.CN_POSTMAN_ASSIGNED;
            default:
                return ShippingOrder.Status.DELIVERED;
        }
    }

    private static ProductCategory convertProductCategory(final jiaonidaigou.appengine.lib.teddy.model.Product.Category category) {
        if (category == null) {
            return ProductCategory.UNKNOWN;
        }
        switch (category) {
            case LARGE_COMMERCIAL_GOODS:
                return ProductCategory.LARGE_COMMERCIAL_GOODS;
            case HEALTH_SUPPLEMENTS:
                return ProductCategory.HEALTH_SUPPLEMENTS;
            case SMALL_APPLIANCES:
                return ProductCategory.SMALL_APPLIANCES;
            case BABY_PRODUCTS:
                return ProductCategory.BABY_PRODUCTS;
            case MILK_POWDER:
                return ProductCategory.MILK_POWDER;
            case LARGE_ITEMS:
                return ProductCategory.LARGE_ITEMS;
            case MAKE_UP:
                return ProductCategory.MAKE_UP;
            case FOOD:
                return ProductCategory.FOOD;
            case BAGS:
                return ProductCategory.BAGS;
            case BLANK:
                return ProductCategory.UNKNOWN;
            case CLOTHES_AND_SHOES:
                return ProductCategory.CLOTHES;
            case WATCH_AND_ACCESSORIES:
                return ProductCategory.WATCHES;
            case TOYS_AND_DAILY_NECESSITIES:
                return ProductCategory.DAILY_NECESSITIES;
            default:
                return ProductCategory.UNKNOWN;
        }
    }

    private static ShippingOrder.ShippingEnding convertShippingEnding(final Order.DeliveryEnding ending) {
        if (ending == null) {
            return null;
        }
        switch (ending) {
            case UNKNOWN:
                return ShippingOrder.ShippingEnding.UNKNOWN;
            case OTHERS_SIGNED:
                return ShippingOrder.ShippingEnding.OTHERS_SIGNED;
            case PICK_UP_BOX:
                return ShippingOrder.ShippingEnding.PICK_UP_BOX;
            case SELF_SIGNED:
                return ShippingOrder.ShippingEnding.SELF_SIGNED;
            case UNKNOWN_SIGNED:
                return ShippingOrder.ShippingEnding.UNKNOWN_SIGNED;
            default:
                return null;
        }
    }

    private static boolean setIfNotBlank(final String str, Consumer<String> consumer) {
        if (StringUtils.isNotBlank(str)) {
            consumer.accept(str);
            return true;
        }
        return false;
    }

    private static <T> boolean setIfNotNull(final T obj, Consumer<T> consumer) {
        if (obj != null) {
            consumer.accept(obj);
            return true;
        }
        return false;
    }
}