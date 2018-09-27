package jiaonidaigou.appengine.integrationtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.common.test.TestUtils;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import jiaonidaigou.appengine.wiremodel.api.ParseRequest;
import jiaonidaigou.appengine.wiremodel.api.ParseResponse;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.MediaObject;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import static jiaonidaigou.appengine.tools.remote.ApiClient.CUSTOM_SECRET_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParserIntegrationTest {
    private static final String KNOWN_MEDIA_ID_CUSTOMER_ONLY = "f3233a4f-1bdf-4245-b4c4-9f1d9c684edb.jpg";//"0a9982e3-fa1a-4aaa-9bd1-958649492bb5.jpg";

    private final ApiClient apiClient = new ApiClient(Env.DEV);

    @Test
    public void parseCustomerByText() {
        String input = "新地址：上海市长宁区金钟路68弄剑河家苑5号1404，黄桦，13916608921";
        ParseRequest parseRequest = ParseRequest
                .newBuilder()
                .setDomain(ParseRequest.Domain.CUSTOMER)
                .addTexts(input)
                .build();
        ParseResponse parseResponse = apiClient.newTarget()
                .path("/api/parse")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.entity(parseRequest, MediaType.APPLICATION_JSON_TYPE))
                .readEntity(ParseResponse.class);
        assertTrue(parseResponse.getResultsCount() > 0);
        print(parseResponse);
    }

    @Test
    public void parseCustomerByTextWithLimit() {
        String input = "新地址：上海市长宁区金钟路68弄剑河家苑5号1404，黄桦，13916608921";
        ParseRequest parseRequest = ParseRequest
                .newBuilder()
                .setLimit(1)
                .setDomain(ParseRequest.Domain.CUSTOMER)
                .addTexts(input)
                .build();
        ParseResponse parseResponse = apiClient.newTarget()
                .path("/api/parse")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.entity(parseRequest, MediaType.APPLICATION_JSON_TYPE))
                .readEntity(ParseResponse.class);
        assertEquals(1, parseResponse.getResultsCount());
        print(parseResponse);
    }

    @Test
    public void parseCustomerByMediaId() {
        // Parse
        ParseRequest parseRequest = ParseRequest
                .newBuilder()
                .setLimit(1)
                .setDomain(ParseRequest.Domain.CUSTOMER)
                .addMediaIds(KNOWN_MEDIA_ID_CUSTOMER_ONLY)
                .build();
        ParseResponse parseResponse = apiClient.newTarget()
                .path("/api/parse")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.entity(parseRequest, MediaType.APPLICATION_JSON_TYPE))
                .readEntity(ParseResponse.class);

        System.out.println(ObjectMapperProvider.prettyToJson(parseResponse));
    }

    @Test
    public void parseCustomerByImageDirectUpload() {
        byte[] bytes = TestUtils.readResourceAsBytes("customer_only.jpg");

        ParseRequest parseRequest = ParseRequest
                .newBuilder()
                .setDomain(ParseRequest.Domain.CUSTOMER)
                .addDirectUploadImages(ParseRequest.DirectUploadImage.newBuilder()
                        .setExt("jpg")
                        .setBytes(ByteString.copyFrom(bytes)))
                .build();
        ParseResponse parseResponse = apiClient.newTarget()
                .path("/api/parse")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.entity(parseRequest, MediaType.APPLICATION_JSON_TYPE))
                .readEntity(ParseResponse.class);
        assertTrue(parseResponse.getResultsCount() > 0);
        print(parseResponse);
    }

    @Test
    public void parseCustomerByKnownCustomer() {
        Customer customer = apiClient.newTarget()
                .path("/api/JiaoNiDaiGou/customers/getAll")
                .queryParam("limit", 1)
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .get(new GenericType<PaginatedResults<Customer>>() {
                })
                .getResults()
                .get(0);

        ParseRequest parseRequest = ParseRequest
                .newBuilder()
                .setLimit(1)
                .setDomain(ParseRequest.Domain.CUSTOMER)
                .addTexts(customer.getName())
                .build();
        ParseResponse parseResponse = apiClient.newTarget()
                .path("/api/parse")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.entity(parseRequest, MediaType.APPLICATION_JSON_TYPE))
                .readEntity(ParseResponse.class);

        assertEquals(1, parseResponse.getResultsCount());
        assertEquals(customer, parseResponse.getResultsList().get(0).getCustomer());
        print(parseResponse);
    }

    private static <T extends Message> void print(T response) {
        try {
            System.out.println(ObjectMapperProvider.get()
                    .writerWithDefaultPrettyPrinter().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
