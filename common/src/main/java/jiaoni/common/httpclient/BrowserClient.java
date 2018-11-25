package jiaoni.common.httpclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.InternalIOException;
import jiaoni.common.utils.Retrier;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A new implementation for Browser client.
 */
public class BrowserClient implements Closeable {
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = ObjectMapperProvider.get();
    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserClient.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.62 Safari/537.36";
    private static final Retrier DEFAULT_RETRIER = Retrier
            .builder()
            .retryOnAnyException()
            .exponentialJitteredBackOffWaiting(100, 5000)
            .stopWithMaxAttempts(5)
            .build();
    private static final RequestConfig DEFAULT_REQUEST_CONFIG = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
    private final PoolingHttpClientConnectionManager connectionManager;
    private final HttpClient client;

    public BrowserClient() {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.DEFAULT)
                .setDefaultCookieStore(new BasicCookieStore()) // in memory
                .build();
    }

    @VisibleForTesting
    public BrowserClient(final HttpClient client) {
        connectionManager = null;
        this.client = checkNotNull(client);
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

    private static void addHeaderUserAgent(final HttpMessage httpMessage) {
        httpMessage.setHeader("User-Agent", USER_AGENT);
    }

    private static void addHeaders(final HttpMessage httpMessage, final Multimap<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entries()) {
            httpMessage.addHeader(entry.getKey(), entry.getValue());
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

    @Override
    public void close() throws IOException {
        if (connectionManager != null) {
            connectionManager.close();
        }
    }

    @VisibleForTesting
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
        if (httpGet.getFirstHeader("User-Agent") == null) {
            addHeaderUserAgent(httpGet);
        }
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
        if (httpOptions.getFirstHeader("User-Agent") == null) {
            addHeaderUserAgent(httpOptions);
        }
        LOGGER.info("Executing request {}", httpOptions.getRequestLine());
        return execute(httpOptions, handle);
    }

    private <T> T execute(final DoPost request, final HttpEntityHandle<T> handle) {
        HttpEntity entity = request.getBody();
        if (entity == null && !request.formBody.isEmpty()) {
            entity = new UrlEncodedFormEntity(toNameValueParis(request.getFormBody()), Consts.UTF_8);
        }

        String url = appendPathParams(request.getUrl(), request.getPathParams());
        HttpPost httpPost = new HttpPost(url);
        addHeaders(httpPost, request.getHeaders());
        if (httpPost.getFirstHeader("User-Agent") == null) {
            addHeaderUserAgent(httpPost);
        }
        httpPost.setEntity(entity);

        LOGGER.info("Executing request {}. Entity: {}", httpPost.getRequestLine(), httpPost.getEntity());
        return execute(httpPost, handle);
    }

    private <T> T execute(final HttpUriRequest request, final HttpEntityHandle<T> handle) {
        try {
            T toReturn = DEFAULT_RETRIER.call(() -> client.execute(request,
                    responseHandler(request.getMethod(), request.getURI().toString(), handle)));
            return toReturn;
        } catch (Exception e) {
            throw new InternalIOException(e);
        }
    }

    private <T> ResponseHandler<T> responseHandler(final String verb,
                                                   final String url,
                                                   final HttpEntityHandle<T> handle) {
        return response -> {
            LOGGER.info("{}: {}. Response {}", verb, url, response.getStatusLine());
            if (response.getStatusLine().getStatusCode() >= 400) {
                String errorContent = EntityUtils.toString(response.getEntity());
                LOGGER.error("{}:{}. Failed. ERROR: {}", verb, url, errorContent);
                throw new RuntimeException("failed to " + verb + " " + url + ".");
            }

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

        DoPost(BrowserClient client) {
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
        DoGet(BrowserClient client) {
            super(client);
        }
    }

    public static class DoOptions extends DoHttp<DoOptions> {
        DoOptions(BrowserClient client) {
            super(client);
        }
    }

    public static class DoHttp<T extends DoHttp<T>> {
        private final BrowserClient client;
        private final OrderedMap<String, Object> pathParams = new ListOrderedMap<>();
        private final Multimap<String, String> headers = HashMultimap.create();
        private String url;
        private boolean redirect;

        DoHttp(final BrowserClient client) {
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

        public T pathParam(final String name, final Object value) {
            checkArgument(!pathParams.containsKey(name));
            if (value != null) {
                pathParams.put(name, value);
            }
            return (T) this;
        }

        public T redirect(final boolean redirect) {
            this.redirect = redirect;
            return (T) this;
        }

        public HttpCallback request() {
            return new HttpCallback(this);
        }

        Multimap<String, String> getHeaders() {
            return headers;
        }

        Map<String, Object> getPathParams() {
            return pathParams;
        }

        String getUrl() {
            return url;
        }

        boolean isRedirect() {
            return redirect;
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

        public JsonNode callToJsonNode() {
            return callToJsonNode(DEFAULT_OBJECT_MAPPER);
        }

        public JsonNode callToJsonNode(final ObjectMapper objectMapper) {
            return request.client.execute(request, t -> objectMapper.readTree(t.getContent()));
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
            // TODO: is this the best way?
            return Jsoup.parse(callToString());
        }

        public byte[] callToBytes() {
            return request.client.execute(request, EntityUtils::toByteArray);
        }
    }
}
