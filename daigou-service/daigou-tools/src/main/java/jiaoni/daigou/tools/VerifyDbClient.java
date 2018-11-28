package jiaoni.daigou.tools;

import jiaoni.common.appengine.access.db.DbClient;
import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient;
import jiaoni.daigou.wiremodel.entity.ShippingOrder;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Verify {@link DbClient}.
 */
public class VerifyDbClient {
    public static void main(String[] args) throws Exception {
        long limit = DateTime.now().minusDays(6).getMillis();

        try (RemoteApi remoteApi = RemoteApi.login()) {

            ShippingOrderDbClient dbClient = new ShippingOrderDbClient(
                    Env.PROD,
                    remoteApi.getDatastoreService()
            );

            List<ShippingOrder> shippingOrders = dbClient.scan().collect(Collectors.toList());

            List<ShippingOrder> total = new ArrayList<>();
            for (ShippingOrder shippingOrder : shippingOrders) {
                if (StringUtils.isNotBlank(shippingOrder.getTeddyOrderId())
                        && shippingOrder.getCreationTime() > limit) {
                    total.add(shippingOrder);
                }
            }

            double income = total.stream()
                    .map(t -> t.getTotalSellPrice().getValue())
                    .reduce((a, b) -> a + b)
                    .get();
            System.out.println(income);
        }
    }
}

