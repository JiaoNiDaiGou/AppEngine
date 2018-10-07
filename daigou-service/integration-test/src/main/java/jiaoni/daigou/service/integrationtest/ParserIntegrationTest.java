package jiaoni.daigou.service.integrationtest;

import com.google.protobuf.ByteString;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.Env;
import jiaoni.common.test.TestUtils;
import jiaoni.daigou.tools.remote.ApiClient;
import jiaoni.daigou.wiremodel.api.ParseRequest;
import jiaoni.daigou.wiremodel.api.ParseResponse;
import org.junit.Test;

import javax.ws.rs.client.Entity;

import static jiaoni.daigou.tools.remote.ApiClient.CUSTOM_SECRET_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
                .post(Entity.json(parseRequest))
                .readEntity(ParseResponse.class);
        assertTrue(parseResponse.getResultsCount() > 0);
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
                .post(Entity.json(parseRequest))
                .readEntity(ParseResponse.class);
        assertEquals(1, parseResponse.getResultsCount());
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
                .post(Entity.json(parseRequest))
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
                .post(Entity.json(parseRequest))
                .readEntity(ParseResponse.class);
        assertTrue(parseResponse.getResultsCount() > 0);
    }

    @Test
    public void parseCustomerByKnownCustomer() {
        String customerName = "闫海侠";
        ParseRequest parseRequest = ParseRequest
                .newBuilder()
                .setLimit(1)
                .setDomain(ParseRequest.Domain.CUSTOMER)
                .addTexts(customerName)
                .build();
        ParseResponse parseResponse = apiClient.newTarget()
                .path("/api/parse")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.json(parseRequest))
                .readEntity(ParseResponse.class);

        assertEquals(1, parseResponse.getResultsCount());
        assertEquals(customerName, parseResponse.getResultsList().get(0).getCustomer().getName());
    }
}
