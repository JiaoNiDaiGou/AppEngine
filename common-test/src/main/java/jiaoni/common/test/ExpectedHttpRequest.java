package jiaoni.common.test;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie2;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ExpectedHttpRequest {
    private String url;
    private String method;
    private SetMultimap<String, String> headers;
    private Cookie cookie;

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public SetMultimap<String, String> getHeaders() {
        return headers;
    }

    public Cookie getCookie() {
        return cookie;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String url;
        private String method;
        private SetMultimap<String, String> headers = HashMultimap.create();
        private Cookie cookie;

        private Builder() {
        }

        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder withMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder withHeaders(SetMultimap<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder withCookie(Cookie cookie) {
            this.cookie = cookie;
            return this;
        }

        public ExpectedHttpRequest build() {
            ExpectedHttpRequest expectedHttpRequest = new ExpectedHttpRequest();
            expectedHttpRequest.url = this.url;
            expectedHttpRequest.method = this.method;
            expectedHttpRequest.headers = this.headers;
            return expectedHttpRequest;
        }
    }

    /**
     * Load from a req file.
     * A req file format:
     * <pre>
     *     method
     *     URL
     *     (blank)
     *     headers
     *     headers
     *     ...
     *     headers
     *     (blank)
     *
     *     line[0]: method
     *     second line: url
     *
     *
     *
     *     result body:
     *
     * </pre>
     */
    public static ExpectedHttpRequest fromReqFile(URL url) throws IOException {
        List<String> lines = IOUtils.readLines(url.openStream(), Charsets.UTF_8);
        String method = lines.get(0).toUpperCase();
        String requestUrl = lines.get(1);

        int lineCnt = 3;

        // Headers
        SetMultimap<String, String> headers = HashMultimap.create();
        BasicClientCookie2 cookie = new BasicClientCookie2("name?", "vs");

        while (lineCnt < lines.size()) {
            String line = lines.get(lineCnt);
            lineCnt++;
            if (line.startsWith("#")) {
                continue;
            }
            if (StringUtils.isBlank(line)) {
                break;
            }
            String headerName = StringUtils.substringBefore(line, ":");
            String rawVal = StringUtils.substringAfter(line, ":").trim();
            if (SINGLE_LINE_HEADER_NAMES.contains(headerName)) {
                headers.put(headerName, rawVal);
            } else if ("Cookie".equals(headerName)) {
                Arrays.stream(StringUtils.split(rawVal, ";"))
                        .map(StringUtils::trimToNull)
                        .filter(Objects::nonNull)
                        .forEach(x -> {
                            String k = StringUtils.substringBefore(x, "=");
                            String v = StringUtils.substringAfter(x, "=");
                            cookie.setAttribute(k, v);
                        });
            } else {
                Arrays.stream(rawVal.split(","))
                        .map(StringUtils::trimToNull)
                        .filter(Objects::nonNull)
                        .forEach(t -> headers.put(headerName, t));
            }
        }

        return ExpectedHttpRequest.builder()
                .withMethod(method)
                .withUrl(requestUrl)
                .withHeaders(headers)
                .withCookie(cookie)
                .build();
    }

    private static final Set<String> SINGLE_LINE_HEADER_NAMES = Sets.newHashSet("User-Agent");
}
