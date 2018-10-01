package jiaonidaigou.appengine.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;
import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DumpTeddy2 {
    public static void main(String[] args) throws Exception {
        File folder = new File("/Users/ruijie.fu/tmp/great_dump");
        Map<String, ShippingOrder> map = new HashMap<>();
        for (File file : folder.listFiles()) {
            List<ShippingOrder> shippingOrders = ObjectMapperProvider.get()
                    .readValue(file, new TypeReference<List<ShippingOrder>>() {
                    });
            shippingOrders.forEach(t -> map.put(t.getTeddyOrderId(), t));
        }

        printMaxAndMin(map.values());

        Multimap<String, ShippingOrder> sortByMonth = sortByMonth(map.values());
        for (Map.Entry<String, Collection<ShippingOrder>> entry : sortByMonth.asMap().entrySet()) {
            File file = new File("/Users/ruijie.fu/tmp/dump2/" + entry.getKey() + ".json");
            List<ShippingOrder> shippingOrders = new ArrayList<>(entry.getValue());
            shippingOrders.sort(comparator);
//            ObjectMapperProvider.get().writeValue(file, shippingOrders);
        }
    }

    private static Comparator<ShippingOrder> comparator = (o1, o2) -> {
        Long ida = Long.parseLong(o1.getTeddyOrderId());
        Long idb = Long.parseLong(o2.getTeddyOrderId());
        return Long.compare(ida, idb);
    };

    private static void printMaxAndMin(Collection<ShippingOrder> shippingOrders) {
        String minId = shippingOrders.stream().min(comparator).get().getTeddyOrderId();
        String maxId = shippingOrders.stream().max(comparator).get().getTeddyOrderId();
        System.out.println(minId);
        System.out.println(maxId);
    }

    private static Multimap<String, ShippingOrder> sortByMonth(Collection<ShippingOrder> shippingOrders) {
        Multimap<String, ShippingOrder> map = ArrayListMultimap.create();
        for (ShippingOrder shippingOrder : shippingOrders) {
            DateTime creationTime = new DateTime(shippingOrder.getCreationTime());
            String month = creationTime.toString("yyyy_MM");
            map.put(month, shippingOrder);
        }
        return map;
    }
}
