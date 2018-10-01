package jiaonidaigou.appengine.tools.remote;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.common.utils.EncryptUtils;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.common.utils.Secrets;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.io.Closeable;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;

public class ApiClient implements Closeable {
    private static final String API_CLIENT_OP_NAME_HEADER = "X-JNDG-ApiClientOp";
    public static final String CUSTOM_SECRET_HEADER = "X-JNDG-SEC";

    private static final byte[] CLIENT_KEY;
    private static final byte[] CLIENT_IV;

    static {
        String[] lines = Secrets.of("gae.global.keyAndIv").getAsStringLines();
        CLIENT_KEY = Base64.getDecoder().decode(lines[0].getBytes(Charsets.UTF_8));
        CLIENT_IV = Base64.getDecoder().decode(lines[1].getBytes(Charsets.UTF_8));
    }

    private static final Map<Env, String> HOSTNAMES_BY_ENV = ImmutableMap
            .<Env, String>builder()
            .put(Env.LOCAL, Environments.LOCAL_ENDPOINT)
            .put(Env.DEV, "https://" + Environments.DEV_VERSION_GAE_HOSTNAME)
            .put(Env.PROD, "https://" + Environments.PROD_VERSION_GAE_HOSTNAME)
            .build();

    private static final JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();

    static {
        jacksonProvider.setMapper(ObjectMapperProvider.get());
    }

    private final Client client;
    private final String hostname;

    private final LoadingCache<String, String> MY_GOOGLE_TOKEN = CacheBuilder
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String key) throws Exception {
                    Process process = Runtime.getRuntime().exec("gcloud auth print-access-token");
                    List<String> output;
                    try (Reader reader = new InputStreamReader(process.getInputStream(), Charsets.UTF_8)) {
                        output = CharStreams.readLines(reader);
                    }
                    return output.stream().filter(t -> t.startsWith("ya29")).findFirst()
                            .orElseThrow(() -> new RuntimeException("failed to get google auth token:" + output));
                }
            });

    public ApiClient(final Env env) {
        this(HOSTNAMES_BY_ENV.get(protectProd(env)));
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
        return newTarget(clientOpName, hostname);
    }

    public WebTarget newTarget(final String clientOpName, final String target) {
        WebTarget toReturn = client.target(target);
        if (StringUtils.isNotBlank(clientOpName)) {
            toReturn.register(new ClientOpFilter(clientOpName));
        }
        return toReturn;
    }

    private String getGoogleAuthToken() {
        try {
            return MY_GOOGLE_TOKEN.get("anywork");
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public String getGoogleAuthTokenBearerHeader() {
        return "Bearer " + getGoogleAuthToken();
    }

    public String getCustomSecretHeader() {
        String adminWeUserId = Secrets.of("gae.admin.weUserId").getAsStringLines()[0];
        byte[] bytes = adminWeUserId.getBytes(Charsets.UTF_8);
        bytes = EncryptUtils.aesEncrypt(CLIENT_KEY, CLIENT_IV, bytes);
        return Base64.getEncoder().encodeToString(bytes);
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

    protected static Env protectProd(final Env env) {
        if (env == Env.PROD) {
            throw new IllegalStateException("cannot hit prod");
        }
        return env;
    }
}
