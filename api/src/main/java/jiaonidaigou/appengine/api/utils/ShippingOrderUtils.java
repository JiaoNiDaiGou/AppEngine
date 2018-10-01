package jiaonidaigou.appengine.api.utils;

import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;

import java.util.Comparator;

public class ShippingOrderUtils {
    public static Comparator<ShippingOrder> comparatorByTeddyOrderIdAsc() {
        return (o1, o2) -> {
            Long ida = Long.parseLong(o1.getTeddyOrderId());
            Long idb = Long.parseLong(o2.getTeddyOrderId());
            return Long.compare(ida, idb);
        };
    }
}
