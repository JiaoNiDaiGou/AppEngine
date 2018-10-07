package jiaonidaigou.appengine.api.utils;

import jiaoni.daigou.wiremodel.entity.ShippingOrder;

import java.util.Comparator;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ShippingOrderUtils {
    public static Comparator<ShippingOrder> comparatorByTeddyOrderIdAsc() {
        return (o1, o2) -> {
            Long ida = Long.parseLong(o1.getTeddyOrderId());
            Long idb = Long.parseLong(o2.getTeddyOrderId());
            return Long.compare(ida, idb);
        };
    }

    /**
     * Merge a base ShippingOrder with a synced order from Teddy.
     */
    public static ShippingOrder mergeSyncedOrder(final ShippingOrder base,
                                                 final ShippingOrder synced) {
        ShippingOrder.Builder builder = base.toBuilder()
                .setStatus(synced.getStatus());
        if (!builder.hasReceiver() && synced.hasReceiver()) {
            builder.setReceiver(synced.getReceiver());
        }
        if (builder.getCreationTime() == 0) {
            builder.setCreationTime(synced.getCreationTime());
        }
        if (builder.getProductEntriesCount() == 0) {
            // Only set when existing has no product info.
            builder.addAllProductEntries(synced.getProductEntriesList());
        }
        if (isBlank(builder.getProductSummary()) && isNotBlank(synced.getProductSummary())) {
            builder.setProductSummary(synced.getProductSummary());
        }
        if (builder.getTotalWeightLb() == 0 && synced.getTotalWeightLb() != 0) {
            builder.setTotalWeightLb(synced.getTotalWeightLb());
        }
        if (!builder.hasPostman() && synced.hasPostman()) {
            builder.setPostman(synced.getPostman());
        }
        if (isBlank(builder.getTrackingNumber()) && isNotBlank(synced.getTrackingNumber())) {
            builder.setTrackingNumber(synced.getTrackingNumber());
        }
        if (isBlank(builder.getShippingCarrier()) && isNotBlank(synced.getShippingCarrier())) {
            builder.setShippingCarrier(synced.getShippingCarrier());
        }
        if (builder.getShippingEnding() == ShippingOrder.ShippingEnding.UNRECOGNIZED &&
                synced.getShippingEnding() != ShippingOrder.ShippingEnding.UNRECOGNIZED) {
            builder.setShippingEnding(synced.getShippingEnding());
        }
        if (synced.getShippingHistoryCount() > 0) {
            // Always sync it
            builder.clearShippingHistory().addAllShippingHistory(synced.getShippingHistoryList());
        }
        if (isBlank(builder.getTeddyOrderId()) && isNotBlank(synced.getTeddyOrderId())) {
            builder.setTeddyOrderId(synced.getTeddyOrderId());
            builder.setTeddyFormattedId(synced.getTeddyFormattedId());
        }
        if (isBlank(builder.getSenderName()) && isNotBlank(synced.getSenderName())) {
            builder.setSenderName(synced.getSenderName());
        }
        if (!builder.hasTotalPrice() && synced.hasTotalPrice()) {
            builder.setTotalPrice(synced.getTotalPrice());
        }
        if (!builder.hasShippingFee() && synced.hasShippingFee()) {
            builder.setShippingFee(synced.getShippingFee());
        }
        return builder.build();
    }
}
