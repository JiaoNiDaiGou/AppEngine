package jiaoni.common.httpclient;

import org.apache.http.cookie.Cookie;

import java.util.List;

/**
 * Data access to browser cookie.
 */
public interface CookieDao {
    /**
     * Save cookies for given app name.
     */
    void save(final String appName, final List<Cookie> cookies);

    /**
     * Load cookies by given app name.
     */
    List<Cookie> load(final String appName);
}
