package jiaonidaigou.appengine.common.httpclient;

import org.apache.http.cookie.Cookie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryCookieStore implements CookieDao {
    private static Map<String, List<Cookie>> STORE = new HashMap<>();

    @Override
    public void save(String appName, List<Cookie> cookies) {
        STORE.put(appName, cookies);
    }

    @Override
    public List<Cookie> load(String appName) {
        return STORE.get(appName);
    }
}
