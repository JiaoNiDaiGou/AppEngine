package jiaoni.daigou.lib.wx;

import jiaoni.common.httpclient.BrowserClient;
import jiaoni.common.test.MockHttpClient;
import org.junit.Before;
import org.junit.Test;

import static jiaoni.common.test.TestUtils.readResourceAsExpectedHttpRequest;
import static jiaoni.common.test.TestUtils.readResourcesAsString;
import static jiaoni.common.test.TestUtils.verifyHttpExecute;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;

public class WxClientImplTest {
    private MockHttpClient client;
    private WxClientImpl underTest;

    @Before
    public void setUp() {
        client = spy(new MockHttpClient());
        underTest = new WxClientImpl(new BrowserClient(client));
    }

    @Test
    public void testFetchLoginUuid() throws Exception {
        client.arrangeResponse(200, readResourcesAsString("jslogin.res.txt"));

        String uuid = underTest.fetchLoginUuid();

        assertEquals("Qa17NgNO-g==", uuid);

        verifyHttpExecute(client, readResourceAsExpectedHttpRequest("jslogin.req.txt"));
    }
}
