package jiaoni.lib.teddy;

import jiaoni.common.httpclient.MockBrowserClient;
import jiaoni.lib.teddy.model.OrderPreview;
import jiaoni.lib.teddy.model.Admin;
import jiaoni.lib.teddy.model.Order;
import jiaoni.lib.teddy.model.Product;
import jiaoni.lib.teddy.model.Receiver;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static jiaoni.common.test.TestUtils.doReturnStringFromResource;
import static jiaoni.common.test.TestUtils.mockBrowserClient;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class TeddyClientTest {
    private MockBrowserClient client;
    private TeddyClientImpl underTest;

    @Before
    public void setUp() {
        client = mockBrowserClient();
        Admin admin = Admin.builder()
                .withLoginUsername("test-admin")
                .build();
        underTest = new TeddyClientImpl(admin, client);
    }

    @Test
    public void testLogin() {
        arrangeClient("home_page", "after_login_page");
        underTest.login();
        assertTrue(underTest.isLoggedIn());
    }

    @Test
    public void testAutoLogin() {
        arrangeClient("ask_login_page", "home_page", "after_login_page");
        underTest.login();
        assertTrue(underTest.isLoggedIn());
    }

    @Test
    public void testGetReceiversByPageNum() {
        arrangeClient("receiver_list");
        List<Receiver> receivers = underTest.getReceivers(1);

        assertEquals(20, receivers.size());
        for (Receiver receiver : receivers) {
            assertNotNull(receiver.getUserId());
            assertNotNull(receiver.getName());
            assertNotNull(receiver.getPhone());
            assertNotNull(receiver.getAddressRegion());
            assertNotNull(receiver.getAddressCity());
            assertNotNull(receiver.getAddressZone());
            assertNotNull(receiver.getAddress());
        }
    }

    @Test
    public void testGetOrderDetails_withShippingInfo() {
        arrangeClient("order_view_created", "shipping_history");
        Order order = underTest.getOrderDetails(93036, true);

        assertTrue(order.getId() > 0);
        assertNotNull(order.getFormattedId());
        assertNotNull(order.getCreationTime());
        assertNotNull(order.getReceiver());
        assertNotNull(order.getSenderName());
        assertNotNull(order.getIdCardUploaded());
        assertTrue(order.getProducts().size() > 0);
        assertFalse(order.isDelivered());
        assertNotNull(order.getPostmanName());
        assertNotNull(order.getPostmanPhone());
        assertTrue(order.getShippingHistory().size() > 0);
        assertEquals("9975123810600", order.getTrackingNumber());
        assertEquals("邮政平邮", order.getRawShippingStatus());
        assertEquals(Order.Status.DELIVERED, order.getStatus());
        assertEquals(Order.DeliveryEnding.PICK_UP_BOX, order.getDeliveryEnding());
    }

    @Test
    public void testGetOrderDetails_noShippingInfo_created() {
        arrangeClient("order_view_created");
        Order order = underTest.getOrderDetails(93036, false);

        assertTrue(order.getId() > 0);
        assertNotNull(order.getFormattedId());
        assertNotNull(order.getCreationTime());
        assertNotNull(order.getReceiver());
        assertNotNull(order.getSenderName());
        assertNotNull(order.getIdCardUploaded());
        assertEquals(1, order.getProducts().size());
        Product product = order.getProducts().get(0);
        assertEquals(Product.Category.HEALTH_SUPPLEMENTS, product.getCategory());
        assertEquals("维生素b族", product.getName());
        assertEquals("kirkland", product.getBrand());
        assertEquals(Order.Status.CREATED, order.getStatus());
        assertNull(order.getShippingFee());
        assertEquals(1.5d, order.getTotalWeight(), 0d);
    }

    @Test
    public void testGetOrderDetails_noShippingInfo_pending() {
        arrangeClient("order_view_pending");
        Order order = underTest.getOrderDetails(138359, false);

        assertTrue(order.getId() > 0);
        assertNotNull(order.getFormattedId());
        assertNotNull(order.getCreationTime());
        assertNotNull(order.getReceiver());
        assertNotNull(order.getSenderName());
        assertNotNull(order.getIdCardUploaded());
        assertEquals(1, order.getProducts().size());
        Product product = order.getProducts().get(0);
        assertEquals(Product.Category.HEALTH_SUPPLEMENTS, product.getCategory());
        assertEquals("胶原蛋白软糖", product.getName());
        assertEquals("Costco", product.getBrand());
        assertEquals(Order.Status.PENDING, order.getStatus());
        assertEquals(5.2d, order.getShippingFee(), 0d);
        assertEquals(1.3d, order.getTotalWeight(), 0d);
    }

    @Test
    public void testGetOrderPreview() {
        arrangeClient("order_preview_list");
        Map<Long, OrderPreview> orderPreviews = underTest.getOrderPreviews(1);

        assertEquals(40, orderPreviews.size());
        int numOfOrderWithTrackingNumber = 0;
        for (OrderPreview preview : orderPreviews.values()) {
            assertTrue(preview.getId() > 0);
            assertNotNull(preview.getFormattedId());
            assertNotNull(preview.getLastUpdatedTime());
            assertNotNull(preview.getProductSummary());
            assertTrue(preview.getPrice() > 0);
            assertNotNull(preview.getRawShippingStatus());
            assertNotNull(preview.getRawStatus());
            assertNotNull(preview.getReceiverName());
            if (preview.getRawShippingStatus().contains("邮政")) {
                assertNotNull(preview.getTrackingNumber());
                numOfOrderWithTrackingNumber++;
            }
        }
        assertEquals(21, numOfOrderWithTrackingNumber);
    }

    private void arrangeClient(final String... resourceNames) {
        String[] resources = Arrays.stream(resourceNames)
                .map(t -> "teddy_responses/" + t + ".html")
                .toArray(String[]::new);
        doReturnStringFromResource(resources).when(client).execute(any(), any());
    }
}
