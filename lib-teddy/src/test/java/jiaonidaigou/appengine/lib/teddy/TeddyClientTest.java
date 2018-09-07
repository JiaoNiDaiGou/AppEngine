package jiaonidaigou.appengine.lib.teddy;

import jiaonidaigou.appengine.common.httpclient.MockBrowserClient;
import jiaonidaigou.appengine.lib.teddy.model.Admin;
import jiaonidaigou.appengine.lib.teddy.model.Order;
import jiaonidaigou.appengine.lib.teddy.model.Receiver;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static jiaonidaigou.appengine.common.test.TestUtils.doReturnStringFromResource;
import static jiaonidaigou.appengine.common.test.TestUtils.mockBrowserClient;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
        Map<String, Receiver> receivers = underTest.getReceivers(1);

        assertEquals(20, receivers.size());
        for (Receiver receiver : receivers.values()) {
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
        arrangeClient("order_view", "shipping_history");
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
        assertEquals(Order.Status.POSTMAN_ASSIGNED, order.getStatus());
    }

    @Test
    public void testGetOrderDetails_noShippingInfo() {
        arrangeClient("order_view");
        Order order = underTest.getOrderDetails(93036, false);

        assertTrue(order.getId() > 0);
        assertNotNull(order.getFormattedId());
        assertNotNull(order.getCreationTime());
        assertNotNull(order.getReceiver());
        assertNotNull(order.getSenderName());
        assertNotNull(order.getIdCardUploaded());
        assertTrue(order.getProducts().size() > 0);
        assertEquals(Order.Status.PENDING, order.getStatus());

        System.out.println(order);
    }

    private void arrangeClient(final String... resourceNames) {
        String[] resources = Arrays.stream(resourceNames)
                .map(t -> "teddy_responses/" + t + ".html")
                .toArray(String[]::new);
        doReturnStringFromResource(resources).when(client).execute(any(), any());
    }
}
