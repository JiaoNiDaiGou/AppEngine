package jiaoni.daigou.service.appengine.utils;

import com.google.common.collect.ImmutableSet;
import jiaoni.daigou.lib.teddy.model.Order;
import jiaoni.daigou.lib.teddy.model.OrderPreview;
import jiaoni.daigou.lib.teddy.model.Receiver;
import jiaoni.daigou.lib.teddy.model.ShippingHistoryEntry;
import jiaoni.common.wiremodel.Address;
import jiaoni.daigou.wiremodel.entity.Customer;
import jiaoni.common.wiremodel.PhoneNumber;
import jiaoni.daigou.wiremodel.entity.Postman;
import jiaoni.common.wiremodel.Price;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.daigou.wiremodel.entity.ProductCategory;
import jiaoni.daigou.wiremodel.entity.ShippingOrder;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
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

    public static ShippingOrder convertToShippingOrderFromOrderPreview(final OrderPreview preview) {
        ShippingOrder.Builder builder = ShippingOrder.newBuilder()
                .setStatus(ShippingOrder.Status.EXTERNAL_SHPPING_PENDING);
        setIfNotBlank(String.valueOf(preview.getId()), builder::setTeddyOrderId);
        setIfNotBlank(preview.getFormattedId(), builder::setTeddyFormattedId);
        setIfNotBlank(preview.getReceiverName(), t -> builder.getReceiverBuilder().setName(t));
        setIfNotBlank(preview.getProductSummary(), builder::setProductSummary);
        if (preview.getPrice() > 0) {
            builder.setTotalPrice(Price.newBuilder().setUnit(Price.Unit.USD).setValue(preview.getPrice()));
        }
        if (StringUtils.isNotBlank(preview.getTrackingNumber())) {
            builder.setStatus(ShippingOrder.Status.CN_TRACKING_NUMBER_ASSIGNED)
                    .setTrackingNumber(preview.getTrackingNumber())
                    .setShippingCarrier(preview.getRawShippingStatus());
        } else if ("运单创建成功".equals(preview.getRawShippingStatus())) {
            builder.setStatus(ShippingOrder.Status.EXTERNAL_SHIPPING_CREATED);
        } else {
            builder.setStatus(ShippingOrder.Status.EXTERNAL_SHPPING_PENDING);
        }
        return builder.build();
    }

    public static ShippingOrder convertToShippingOrder(final Order order) {
        ShippingOrder.Builder builder = ShippingOrder.newBuilder()
                .setStatus(convertToShippingOrderStatus(order.getStatus()))
                .setReceiver(convertToCustomer(order.getReceiver()))
                .setCreationTime(order.getCreationTime().getMillis());
        setIfNotBlank(order.getSenderName(), builder::setSenderName);
        setIfNotNull(convertToPostman(order), builder::setPostman);
        setIfNotBlank(order.getTrackingNumber(), builder::setTrackingNumber);
        setIfNotBlank(order.getRawShippingStatus(), builder::setShippingCarrier);
        setIfNotNull(convertToShippingEnding(order.getDeliveryEnding()), builder::setShippingEnding);
        setIfNotNull(order.getTotalWeight(), builder::setTotalWeightLb);
        setIfNotNull(order.getShippingFee(), t -> Price.newBuilder().setUnit(Price.Unit.USD).setValue(t).build());
        return builder.addAllProductEntries(
                order.getProducts()
                        .stream()
                        .map(TeddyUtils::convertToShippingOrderProductEntry)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .setProductSummary(order.getProductSummary())
                .addAllShippingHistory(
                        order.getShippingHistory()
                                .stream()
                                .map(TeddyUtils::convertToShippingOrderShippingHistory)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()))
                .setTeddyOrderId(String.valueOf(order.getId()))
                .setTeddyFormattedId(order.getFormattedId())
                .build();
    }

    private static ShippingOrder.ShippingHistoryEntry convertToShippingOrderShippingHistory(
            final ShippingHistoryEntry entry) {
        return ShippingOrder.ShippingHistoryEntry.newBuilder()
                .setStatus(entry.getStatus())
                .setTimestamp(entry.getTimestamp().getMillis())
                .build();
    }

    private static ShippingOrder.ProductEntry convertToShippingOrderProductEntry(
            final jiaoni.daigou.lib.teddy.model.Product product) {
        Product.Builder productBuilder = Product.newBuilder();
        boolean hasBrand = setIfNotBlank(product.getBrand(), productBuilder::setBrand);
        boolean hasCategory = setIfNotNull(convertProductCategory(product.getCategory()), productBuilder::setCategory);
        boolean hasProductName = setIfNotBlank(product.getName(), productBuilder::setName);
        if (!hasBrand && !hasCategory && !hasProductName) {
            return null;
        }
        return ShippingOrder.ProductEntry.newBuilder()
                .setProduct(productBuilder)
                .setQuantity(product.getQuantity())
                .setSellPrice(Price.newBuilder().setUnit(Price.Unit.USD).setValue(product.getUnitPriceInDollers()))
                .build();
    }

    private static Customer convertToCustomer(final Receiver receiver) {
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

    private static Postman convertToPostman(final Order order) {
        if (isNotBlank(order.getPostmanName()) && isNotBlank(order.getPostmanPhone())) {
            return Postman.newBuilder()
                    .setName(order.getPostmanName())
                    .setPhone(PhoneNumber.newBuilder().setCountryCode("86").setPhone(order.getPostmanPhone()).build())
                    .build();
        }
        return null;
    }

    private static ShippingOrder.Status convertToShippingOrderStatus(final Order.Status status) {
        if (status == null) {
            return ShippingOrder.Status.DELIVERED;
        }
        switch (status) {
            case CREATED:
                return ShippingOrder.Status.EXTERNAL_SHIPPING_CREATED;
            case PENDING:
                return ShippingOrder.Status.EXTERNAL_SHPPING_PENDING;
            case POSTMAN_ASSIGNED:
                return ShippingOrder.Status.CN_TRACKING_NUMBER_ASSIGNED;
            case TRACKING_NUMBER_ASSIGNED:
                return ShippingOrder.Status.CN_POSTMAN_ASSIGNED;
            default:
                return ShippingOrder.Status.DELIVERED;
        }
    }

    private static ProductCategory convertProductCategory(final jiaoni.daigou.lib.teddy.model.Product.Category category) {
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

    private static ShippingOrder.ShippingEnding convertToShippingEnding(final Order.DeliveryEnding ending) {
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

    public static List<jiaoni.daigou.lib.teddy.model.Product> convertToTeddyProducts(
            final List<ShippingOrder.ProductEntry> productEntries) {
        return productEntries
                .stream()
                .map(TeddyUtils::convertToTeddyProduct)
                .collect(Collectors.toList());
    }

    public static jiaoni.daigou.lib.teddy.model.Product convertToTeddyProduct(
            final ShippingOrder.ProductEntry productEntry) {
        return jiaoni.daigou.lib.teddy.model.Product.builder()
                .withBrand(productEntry.getProduct().getBrand())
                .withCategory(convertToTeddyProductCategory(productEntry.getProduct().getCategory()))
                .withName(productEntry.getProduct().getName())
                .withQuantity(productEntry.getQuantity())
                .withUnitPriceInDollers((int) productEntry.getSellPrice().getValue())
                .build();
    }

    public static jiaoni.daigou.lib.teddy.model.Product.Category convertToTeddyProductCategory(
            final ProductCategory category) {
        switch (category) {
            case BAGS:
                return jiaoni.daigou.lib.teddy.model.Product.Category.BAGS;
            case FOOD:
                return jiaoni.daigou.lib.teddy.model.Product.Category.FOOD;
            case TOYS:
            case DAILY_NECESSITIES:
                return jiaoni.daigou.lib.teddy.model.Product.Category.TOYS_AND_DAILY_NECESSITIES;
            case SHOES:
            case CLOTHES:
                return jiaoni.daigou.lib.teddy.model.Product.Category.CLOTHES_AND_SHOES;
            case MAKE_UP:
                return jiaoni.daigou.lib.teddy.model.Product.Category.MAKE_UP;
            case WATCHES:
            case ACCESSORIES:
                return jiaoni.daigou.lib.teddy.model.Product.Category.WATCH_AND_ACCESSORIES;
            case LARGE_ITEMS:
                return jiaoni.daigou.lib.teddy.model.Product.Category.LARGE_ITEMS;
            case MILK_POWDER:
                return jiaoni.daigou.lib.teddy.model.Product.Category.MILK_POWDER;
            case BABY_PRODUCTS:
                return jiaoni.daigou.lib.teddy.model.Product.Category.BABY_PRODUCTS;
            case SMALL_APPLIANCES:
                return jiaoni.daigou.lib.teddy.model.Product.Category.SMALL_APPLIANCES;
            case HEALTH_SUPPLEMENTS:
                return jiaoni.daigou.lib.teddy.model.Product.Category.HEALTH_SUPPLEMENTS;
            case LARGE_COMMERCIAL_GOODS:
                return jiaoni.daigou.lib.teddy.model.Product.Category.LARGE_COMMERCIAL_GOODS;
            case UNKNOWN:
            case UNRECOGNIZED:
            default:
                throw new IllegalStateException("unexpected product category " + category);
        }
    }

    public static Receiver convertToTeddyReceiver(final Customer customer) {
        return Receiver.builder()
                .withUserId(customer.getSocialContacts().getTeddyUserId())
                .withAddressRegion(customer.getAddressesCount() > 0 ? customer.getAddresses(0).getRegion() : null)
                .withAddressCity(customer.getAddressesCount() > 0 ? customer.getAddresses(0).getCity() : null)
                .withAddressZone(customer.getAddressesCount() > 0 ? customer.getAddresses(0).getZone() : null)
                .withAddress(customer.getAddressesCount() > 0 ? customer.getAddresses(0).getAddress() : null)
                .withName(customer.getName())
                .withPhone(customer.getPhone().getPhone())
                .build();
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
