package jiaoni.common.test;

import com.google.common.base.Charsets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MockHttpClient implements HttpClient {
    private ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
    private List<Pair<Integer, String>> arrangedResponses = new ArrayList<>();
    private int callCount;

    public MockHttpClient arrangeResponse(final int code, final String content) {
        arrangedResponses.add(Pair.of(code, content));
        return this;
    }

    public MockHttpClient arrangeResponse(final int code) {
        if (code >= 400) {
            arrangedResponses.add(Pair.of(code, "error code " + code));
        } else {
            arrangedResponses.add(Pair.of(code, ""));
        }
        return this;
    }

    @Override
    public HttpParams getParams() {
        return null;
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return null;
    }

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
        return getArrangedHttpResponse();
    }

    @Override
    public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException {
        return getArrangedHttpResponse();
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
        return getArrangedHttpResponse();
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
        return getArrangedHttpResponse();
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return responseHandler.handleResponse(getArrangedHttpResponse());
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        return responseHandler.handleResponse(getArrangedHttpResponse());
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return responseHandler.handleResponse(getArrangedHttpResponse());
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        return responseHandler.handleResponse(getArrangedHttpResponse());
    }

    private HttpResponse getArrangedHttpResponse() {
        if (callCount >= arrangedResponses.size()) {
            return buildHttpResponse(500, "no arranged http response. arranged.size="
                    + arrangedResponses.size()
                    + ", cur callCount="
                    + callCount);
        }
        Pair<Integer, String> arrangedResponse = arrangedResponses.get(callCount++);
        return buildHttpResponse(arrangedResponse.getKey(), arrangedResponse.getRight());
    }

    private HttpResponse buildHttpResponse(final int code, final String content) {
        BasicHttpEntity entity = new BasicHttpEntity();
        byte[] bytes = content.getBytes(Charsets.UTF_8);
        entity.setContent(new ByteArrayInputStream(bytes));
        entity.setContentLength(bytes.length);
        HttpResponse response = DefaultHttpResponseFactory.INSTANCE
                .newHttpResponse(protocolVersion, code, new BasicHttpContext());
        response.setEntity(entity);
        return response;
    }
}
