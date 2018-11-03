package jiaoni.daigou.tools;

import jiaoni.common.httpclient.BrowserClient;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClient;
import jiaoni.daigou.lib.teddy.TeddyClientImpl;
import jiaoni.daigou.lib.teddy.model.Order;
import jiaoni.daigou.service.appengine.impls.teddy.TeddyUtils;
import jiaoni.daigou.wiremodel.entity.ShippingOrder;

public class VerifyGetTeddyOrderById {
    public static void main(String[] args) throws Exception {
        TeddyClient teddyClient = new TeddyClientImpl(TeddyAdmins.JIAONI, new BrowserClient());
        Order order = teddyClient.getOrderDetails(128287, true);
        ShippingOrder shippingOrder = TeddyUtils.convertToShippingOrder(order);
        System.out.println(ObjectMapperProvider.get().writerWithDefaultPrettyPrinter().writeValueAsString(shippingOrder));
    }
}
