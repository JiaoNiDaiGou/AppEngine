package jiaonidaigou.appengine.tools;

import jiaonidaigou.appengine.api.utils.TeddyUtils;
import jiaonidaigou.appengine.common.httpclient.MockBrowserClient;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClient;
import jiaonidaigou.appengine.lib.teddy.TeddyClientImpl;
import jiaonidaigou.appengine.lib.teddy.model.Order;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;

public class VerifyGetTeddyOrderById {
    public static void main(String[] args) throws Exception {
        TeddyClient teddyClient = new TeddyClientImpl(TeddyAdmins.JIAONI, new MockBrowserClient("jiaoni"));
        Order order = teddyClient.getOrderDetails(128287, true);
        ShippingOrder shippingOrder = TeddyUtils.convertShippingOrder(order);
        System.out.println(ObjectMapperProvider.get().writerWithDefaultPrettyPrinter().writeValueAsString(shippingOrder));
    }
}
