package jiaoni.common.test;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ExpectedHttpRequest {
    private String url;
    private String method;

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String url;
        private String method;

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

        public ExpectedHttpRequest build() {
            ExpectedHttpRequest expectedHttpRequest = new ExpectedHttpRequest();
            expectedHttpRequest.url = this.url;
            expectedHttpRequest.method = this.method;
            return expectedHttpRequest;
        }
    }

    /**
     * Load from a req file.
     * A req file format:
     * <pre>
     *     first line: method
     *
     *     second line: url
     *
     *     result body:
     *
     * </pre>
     */
    public static ExpectedHttpRequest fromReqFile(URL url) throws IOException {
        List<String> lines = IOUtils.readLines(url.openStream(), Charsets.UTF_8);
        String method = lines.get(0).toUpperCase();
        String requestUrl = lines.get(1);

        return ExpectedHttpRequest.builder()
                .withMethod(method)
                .withUrl(requestUrl)
                .build();
    }
}
