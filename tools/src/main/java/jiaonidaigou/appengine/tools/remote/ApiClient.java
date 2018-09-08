package jiaonidaigou.appengine.tools.remote;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.utils.Environments;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.io.Closeable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;

public class ApiClient implements Closeable {
    private static final String SCHEME = "https";
    private static final String API_CLIENT_OP_NAME_HEADER = "API_CLIENT_OP";

    private static final JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();

    static {
        jacksonProvider.setMapper(ObjectMapperProvider.get());
    }

    private final Client client;
    private final String hostname;

    public ApiClient() {
        this(Environments.GAE_HOSTNAME);
    }

    public ApiClient(final String hostname) {
        this.hostname = hostname;
        client = ClientBuilder.newBuilder()
                .register(JacksonFeature.class)
                .register(jacksonProvider)
                .property(ClientProperties.CONNECT_TIMEOUT, 30000)
                .property(ClientProperties.READ_TIMEOUT, 60000)
                .property(ClientProperties.FOLLOW_REDIRECTS, false)
                .build();
    }

    public WebTarget newTarget() {
        return newTarget("unknownClientOp");
    }

    public WebTarget newTarget(final String clientOpName) {
        return newTarget(clientOpName, SCHEME + "://" + hostname);
    }

    public WebTarget newTarget(final String clientOpName, final String target) {
        WebTarget toReturn = client.target(target);
        if (StringUtils.isNotBlank(clientOpName)) {
            toReturn.register(new ClientOpFilter(clientOpName));
        }
        return toReturn;
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    private static class ClientOpFilter implements ClientRequestFilter {
        private final String testName;

        ClientOpFilter(final String testName) {
            this.testName = testName;
        }

        @Override
        public void filter(final ClientRequestContext requestContext) {
            requestContext.getHeaders().putSingle(API_CLIENT_OP_NAME_HEADER, testName);
        }
    }
}
