package jiaoni.common.appengine.access.db;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PageTokenTest {
    @Test
    public void testToAndFromPageToken() {
        PageToken pageToken = PageToken.datastore("token");
        String pageTokenStr = pageToken.toPageToken();

        PageToken deserializedPageToken = PageToken.fromPageToken(pageTokenStr);
        assertEquals(pageToken, deserializedPageToken);
    }
}
