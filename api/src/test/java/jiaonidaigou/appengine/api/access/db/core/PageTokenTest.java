package jiaonidaigou.appengine.api.access.db.core;

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
