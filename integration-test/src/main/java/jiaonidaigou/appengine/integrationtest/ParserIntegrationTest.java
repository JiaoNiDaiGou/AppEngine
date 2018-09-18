package jiaonidaigou.appengine.integrationtest;

import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import jiaonidaigou.appengine.wiremodel.api.ParseRequest;
import jiaonidaigou.appengine.wiremodel.api.ParseResponse;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

public class ParserIntegrationTest {
    private final ApiClient apiClient = new ApiClient(Env.PROD);

    @Test
    public void parseCustomer() throws Exception {
        String input = "新地址：上海市长宁区金钟路68弄剑河家苑5号1404，黄桦，13916608921";
        ParseRequest parseRequest = ParseRequest
                .newBuilder()
                .setDomain(ParseRequest.Domain.CUSTOMER)
                .addTexts(input)
                .build();
        ParseResponse parseResponse = apiClient.newTarget()
                .path("/api/parse")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.entity(parseRequest, MediaType.APPLICATION_JSON_TYPE))
                .readEntity(ParseResponse.class);

        System.out.println(ObjectMapperProvider.get().writerWithDefaultPrettyPrinter().writeValueAsString(parseResponse));
    }
}
