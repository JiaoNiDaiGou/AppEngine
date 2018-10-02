package jiaonidaigou.appengine.api.utils;

import jiaonidaigou.appengine.lib.teddy.model.OrderPreview;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TeddyUtilsTest {
    @Test
    public void testParseRawShippingStatus() {
        String text = "邮政平邮(9975123909012)";
        OrderPreview orderPreview = OrderPreview.builder()
                .withRawShippingStatus(text)
                .build();
        ShippingOrder shippingOrder = TeddyUtils.convertToShippingOrderFromOrderPreview(orderPreview);
        assertEquals("邮政平邮", shippingOrder.getShippingCarrier());
        assertEquals("9975123909012", shippingOrder.getTrackingNumber());
        assertEquals(ShippingOrder.Status.CN_TRACKING_NUMBER_ASSIGNED, shippingOrder.getStatus());
    }
}
