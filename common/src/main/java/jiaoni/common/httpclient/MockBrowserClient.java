package jiaoni.common.httpclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.InternalIOException;
import jiaoni.common.utils.Retrier;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wrapping {@link org.apache.http.impl.client.CloseableHttpClient}
 */
public class MockBrowserClient implements Closeable {
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = ObjectMapperProvider.get();
    private static final Logger LOGGER = LoggerFactory.getLogger(MockBrowserClient.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.62 Safari/537.36";
    private static final Retrier DEFAULT_RETRIER = Retrier
            .builder()
            .retryOnAnyException()
            .exponentialJitteredBackOffWaiting(100, 5000)
            .stopWithMaxAttempts(5)
            .build();
    private static final RequestConfig DEFAULT_REQUEST_CONFIG = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();

    private final String appName;
    private final CloseableHttpClient client;
    private final org.apache.http.client.CookieStore cookieStore;
    private final CookieDao cookieStorage;

    public MockBrowserClient(final String appName) {
        this(appName, null);
    }

    public MockBrowserClient(final String appName,
                             final CookieDao cookieStore) {
        this.appName = checkNotNull(appName);
        this.cookieStore = new BasicCookieStore();
        this.client = HttpClients.custom()
                .setDefaultCookieStore(this.cookieStore)
                .setDefaultRequestConfig(DEFAULT_REQUEST_CONFIG)
                .build();

        this.cookieStorage = cookieStore == null ? new FileBasedCookieStore() : cookieStore;
    }

    private static void addHeaderUserAgent(final HttpMessage httpMessage) {
        httpMessage.setHeader("User-Agent", USER_AGENT);
    }

    private static void addHeaders(final HttpMessage httpMessage, final Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpMessage.setHeader(entry.getKey(), entry.getValue());
        }
    }

    private static String appendPathParams(final String url,
                                           final Map<String, Object> params) {
        return appendPathParams(url, toNameValueParis(params));
    }

    private static String appendPathParams(final String url,
                                           final List<NameValuePair> params) {
        if (CollectionUtils.isEmpty(params)) {
            return url;
        }
        String paramStr;
        try {
            paramStr = EntityUtils.toString(new UrlEncodedFormEntity(params, Consts.UTF_8));
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
        return url + "?" + paramStr;
    }

    private static List<NameValuePair> toNameValueParis(final Map<String, Object> map) {
        return map.entrySet()
                .stream().map(t -> new BasicNameValuePair(t.getKey(), t.getValue().toString()))
                .collect(Collectors.toList());
    }

    public List<Cookie> getCookies() {
        return cookieStore.getCookies();
    }

    public Cookie getCookie(final String cookieName) {
        return cookieStore.getCookies()
                .stream()
                .filter(t -> cookieName.equalsIgnoreCase(t.getName()))
                .findFirst()
                .orElse(null);
    }

    public DoGet doGet() {
        return new DoGet(this);
    }

    public DoPost doPost() {
        return new DoPost(this);
    }

    public DoOptions doOptions() {
        return new DoOptions(this);
    }

    public void saveCookies() {
        cookieStorage.save(appName, getCookies());
    }

    public void loadCookies() {
        List<Cookie> cookies = cookieStorage.load(appName);
        if (cookies != null) {
            cookieStore.clear();
            cookies.forEach(cookieStore::addCookie);
        }
    }

    @Override
    public void close()
            throws IOException {
        if (client != null) {
            client.close();
        }
    }

    public <T> T execute(final DoHttp request, final HttpEntityHandle<T> handle) {
        if (request instanceof DoGet) {
            return execute((DoGet) request, handle);
        } else if (request instanceof DoPost) {
            return execute((DoPost) request, handle);
        } else if (request instanceof DoOptions) {
            return execute((DoOptions) request, handle);
        } else {
            throw new IllegalStateException();
        }
    }

    private <T> T execute(final DoGet request, final HttpEntityHandle<T> handle) {
        String url = appendPathParams(request.getUrl(), request.getPathParams());
        HttpGet httpGet = new HttpGet(url);
        addHeaders(httpGet, request.getHeaders());
        addHeaderUserAgent(httpGet);
        if (!request.isRedirect()) {
            httpGet.setConfig(RequestConfig.copy(DEFAULT_REQUEST_CONFIG).setRedirectsEnabled(false).build());
        }
        LOGGER.info("Executing request {}", httpGet.getRequestLine());
        return execute(httpGet, handle);
    }

    private <T> T execute(final DoOptions request, final HttpEntityHandle<T> handle) {
        String url = appendPathParams(request.getUrl(), request.getPathParams());
        HttpOptions httpOptions = new HttpOptions(url);
        addHeaders(httpOptions, request.getHeaders());
        addHeaderUserAgent(httpOptions);
        LOGGER.info("Executing request {}", httpOptions.getRequestLine());
        return execute(httpOptions, handle);
    }

    private <T> T execute(final DoPost request, final HttpEntityHandle<T> handle) {
        int bodyAssignments = 0;
        bodyAssignments += MapUtils.isEmpty(request.getFormBody()) ? 0 : 1;
        bodyAssignments += request.getBody() == null ? 0 : 1;
        checkArgument(bodyAssignments <= 1,
                "Can only set one of request.body, request.formBody or request.stringBody.");

        String url = appendPathParams(request.getUrl(), request.getPathParams());
        HttpPost httpPost = new HttpPost(url);
        addHeaders(httpPost, request.getHeaders());
        addHeaderUserAgent(httpPost);

        if (request.getBody() != null) {
            httpPost.setEntity(request.getBody());
        } else if (MapUtils.isNotEmpty(request.getFormBody())) {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(toNameValueParis(request.getFormBody()), Consts.UTF_8);
            httpPost.setEntity(entity);
        }
        LOGGER.info("Executing request {}. Entity: {}", httpPost.getRequestLine(), httpPost.getEntity());
        return execute(httpPost, handle);
    }

    private <T> T execute(final HttpUriRequest request, final HttpEntityHandle<T> handle) {
        try {
            return DEFAULT_RETRIER.call(() -> client.execute(request, responseHandler(handle)));
        } catch (Exception e) {
            throw new InternalIOException(e);
        }
    }

    private <T> ResponseHandler<T> responseHandler(final HttpEntityHandle<T> handle) {
        return response -> {
            LOGGER.info("Parsing response {}", response.getStatusLine());
            if (response.getEntity() == null) {
                return null;
            }
            return handle.consume(response.getEntity());
        };
    }

    public interface HttpEntityHandle<T> {
        T consume(final HttpEntity entity) throws IOException;
    }

    public static class DoPost extends DoHttp<DoPost> {
        private final Map<String, Object> formBody = new HashMap<>();
        private HttpEntity body;

        DoPost(MockBrowserClient client) {
            super(client);
        }

        public DoPost formBodyParam(final String name, final Object val) {
            checkArgument(!formBody.containsKey(name));
            if (val != null) {
                formBody.put(name, val);
            }
            return this;
        }

        public DoPost stringBody(final String stringBody) {
            this.body = new StringEntity(stringBody, Consts.UTF_8);
            return this;
        }

        public DoPost jsonBody(final Object jsonBody) {
            try {
                return stringBody(DEFAULT_OBJECT_MAPPER.writeValueAsString(jsonBody));
            } catch (JsonProcessingException e) {
                throw new InternalIOException(e);
            }
        }

        public DoPost body(final HttpEntity body) {
            this.body = body;
            return this;
        }

        Map<String, Object> getFormBody() {
            return formBody;
        }

        HttpEntity getBody() {
            return body;
        }
    }

    public static class DoGet extends DoHttp<DoGet> {
        DoGet(MockBrowserClient client) {
            super(client);
        }
    }

    public static class DoOptions extends DoHttp<DoOptions> {

        DoOptions(MockBrowserClient client) {
            super(client);
        }
    }

    public static class DoHttp<T extends DoHttp<T>> {
        private final MockBrowserClient client;
        private final Map<String, Object> pathParams = new HashMap<>();
        private final Map<String, String> headers = new HashMap<>();
        private String url;
        private boolean redirect;

        DoHttp(final MockBrowserClient client) {
            this.client = checkNotNull(client);
        }

        public T url(final String url) {
            this.url = url;
            return (T) this;
        }

        public T header(final String name, final String value) {
            headers.put(name, value);
            return (T) this;
        }

        public T headerWhen(final Supplier<Boolean> when, final String name, final String value) {
            if (when.get()) {
                headers.put(name, value);
            }
            return (T) this;
        }

        public T pathParam(final String name, final Object value) {
            checkArgument(!pathParams.containsKey(name));
            if (value != null) {
                pathParams.put(name, value);
            }
            return (T) this;
        }

        public T pathParam(final Map<String, Object> pathParams) {
            pathParams.forEach(this::pathParam);
            return (T) this;
        }

        public T redirect(final boolean redirect) {
            this.redirect = redirect;
            return (T) this;
        }

        public HttpCallback request() {
            return new HttpCallback(this);
        }

        public Map<String, Object> getPathParams() {
            return pathParams;
        }

        public String getUrl() {
            return url;
        }

        public boolean isRedirect() {
            return redirect;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }

    public static class HttpCallback {
        private final DoHttp request;

        private HttpCallback(final DoHttp request) {
            this.request = request;
        }

        public HttpEntity call() {
            return request.client.execute(request, t -> t);
        }

        public String callToString() {
            return request.client.execute(request, t -> EntityUtils.toString(t, Consts.UTF_8));
        }

        public <T> T callToJson(final Class<T> type, final ObjectMapper objectMapper) {
            return request.client.execute(request, t -> objectMapper.readValue(t.getContent(), type));
        }

        public <T> T callToJson(final Class<T> type) {
            return callToJson(type, DEFAULT_OBJECT_MAPPER);
        }

        public <T> T callToJson(final TypeReference<T> type) {
            return callToJson(type, DEFAULT_OBJECT_MAPPER);
        }

        public <T> T callToJson(final TypeReference<T> type, final ObjectMapper objectMapper) {
            return request.client.execute(request, t -> objectMapper.readValue(t.getContent(), type));
        }

        public void callToOutputSteam(final OutputStream outputStream) {
            request.client.execute(request, t -> {
                outputStream.write(EntityUtils.toByteArray(t));
                return null;
            });
        }

        public Document callToHtml() {
            return Jsoup.parse(callToString());
        }

        public byte[] callToBytes() {
            return request.client.execute(request, EntityUtils::toByteArray);
        }
    }
}
