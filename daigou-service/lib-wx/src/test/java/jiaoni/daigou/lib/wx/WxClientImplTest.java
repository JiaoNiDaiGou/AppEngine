package jiaoni.daigou.lib.wx;

import jiaoni.common.httpclient.BrowserClient;
import jiaoni.common.test.MockHttpClient;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static jiaoni.common.test.TestUtils.readResourceAsExpectedHttpRequest;
import static jiaoni.common.test.TestUtils.readResourcesAsString;
import static jiaoni.common.test.TestUtils.verifyHttpExecute;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;

public class WxClientImplTest {
    private MockHttpClient client;
    private WxWebClientImpl underTest;

    @Before
    public void setUp() {
        client = spy(new MockHttpClient());
        underTest = new WxWebClientImpl(new BrowserClient(client));
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testFetchLoginUuid() throws Exception {
        DateTimeUtils.setCurrentMillisFixed(1541372698751L);
        client.arrangeResponse(200, readResourcesAsString("jslogin.res.txt"));

        String uuid = underTest.fetchLoginUuid();

        assertEquals("Qa17NgNO-g==", uuid);

        verifyHttpExecute(client, readResourceAsExpectedHttpRequest("jslogin.req.txt"));
    }
}
