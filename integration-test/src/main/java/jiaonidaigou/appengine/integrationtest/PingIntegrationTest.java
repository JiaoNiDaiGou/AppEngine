package jiaonidaigou.appengine.integrationtest;

import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jiaonidaigou.appengine.tools.remote.ApiClient.CUSTOM_SECRET_HEADER;
import static org.junit.Assert.assertEquals;

public class PingIntegrationTest {
    private final ApiClient apiClient = new ApiClient(Env.PROD);

    @Test
    public void pingUnsecure() {
        Response response = apiClient.newTarget()
                .path("/api/ping")
                .queryParam("input", "hello")
                .request()
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        System.out.println(response.readEntity(String.class));
    }

    @Test
    public void pingSecureGoogle() {
        Response response = apiClient.newTarget()
                .path("/api/ping/secure/google")
                .queryParam("input", "hello")
                .request()
                .header(AUTHORIZATION, apiClient.getGoogleAuthTokenBearerHeader())
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        System.out.println(response.readEntity(String.class));
    }

    @Test
    public void pingSecureCustomSecret() {
        Response response = apiClient.newTarget()
                .path("/api/ping/secure/customSecret")
                .queryParam("input", "hello")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        System.out.println(response.readEntity(String.class));
    }
}
