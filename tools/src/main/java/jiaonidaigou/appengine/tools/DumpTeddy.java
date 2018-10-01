package jiaonidaigou.appengine.tools;

import jiaonidaigou.appengine.api.utils.TeddyUtils;
import jiaonidaigou.appengine.common.httpclient.MockBrowserClient;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClient;
import jiaonidaigou.appengine.lib.teddy.TeddyClientImpl;
import jiaonidaigou.appengine.lib.teddy.model.Order;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class DumpTeddy {
    private static final TeddyClient client = new TeddyClientImpl(TeddyAdmins.HACK, new MockBrowserClient("hack"));

    private static int START = 138800;
    private static int PAGE = 500;
    private static int PAGE_START = 2;

    public static void main(String[] args) throws Exception {

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        int page = PAGE_START;
        int start = START - (PAGE_START - 1) * PAGE;

        List<Future<ShippingOrder>> futures = new ArrayList<>();
        for (int i = start; i >= 0; i--) {
            int finalI = i;
            Future<ShippingOrder> future = executorService.submit(() -> dump(finalI));
            futures.add(future);
            if (futures.size() >= 500) {
                List<ShippingOrder> shippingOrders = futures.stream()
                        .map(t -> {
                            try {
                                return t.get();
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                futures.clear();
                File file = new File("/Users/ruijie.fu/tmp/great_dump/great_dump_" + page + ".json");
                ObjectMapperProvider.get().writeValue(file, shippingOrders);
                page++;
                System.out.println(String.format("Saving %s order into %s. end id %s", shippingOrders.size(), file.getName(), i));
            }
        }
    }

    private static ShippingOrder dump(final int id) {
        Order order = client.getOrderDetails(id, false);
        if (order == null) {
            return null;
        }
        return TeddyUtils.convertToShippingOrder(order);
    }
}
