package jiaonidaigou.appengine.integrationtest;

import com.google.protobuf.ByteString;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.common.test.TestUtils;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import jiaonidaigou.appengine.wiremodel.api.ParseRequest;
import jiaonidaigou.appengine.wiremodel.api.ParseResponse;
import jiaonidaigou.appengine.wiremodel.entity.MediaObject;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import static jiaonidaigou.appengine.tools.remote.ApiClient.CUSTOM_SECRET_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.entity(parseRequest, MediaType.APPLICATION_JSON_TYPE))
                .readEntity(ParseResponse.class);
        assertTrue(parseResponse.getResultsCount() > 0);
        System.out.println(ObjectMapperProvider.get().writerWithDefaultPrettyPrinter().writeValueAsString(parseResponse));
    }

    @Test
    public void parseCustomerWithLimit() throws Exception {
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
    }

    @Test
    public void parseCustomerByImageDirectUpload() throws Exception {
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
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.entity(parseRequest, MediaType.APPLICATION_JSON_TYPE))
                .readEntity(ParseResponse.class);
        assertTrue(parseResponse.getResultsCount() > 0);

        System.out.println(ObjectMapperProvider.get()
                .writerWithDefaultPrettyPrinter().writeValueAsString(parseResponse));
    }

    @Test
    public void parseCustomerByImageDirectUpload2() throws Exception {
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
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.entity(parseRequest, MediaType.APPLICATION_JSON_TYPE))
                .readEntity(ParseResponse.class);
        assertTrue(parseResponse.getResultsCount() > 0);

        System.out.println(ObjectMapperProvider.get()
                .writerWithDefaultPrettyPrinter().writeValueAsString(parseResponse));
    }

    @Test
    public void parseCustomerByImageByMediaId() throws Exception {
        byte[] bytes = TestUtils.readResourceAsBytes("customer_only.jpg");

        // Direct upload
        MediaObject uploadObject = apiClient.newTarget()
                .path("api/media/directUpload")
                .queryParam("ext", "jpg")
                .request()
                .header(CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.entity(bytes, MediaType.APPLICATION_OCTET_STREAM_TYPE))
                .readEntity(MediaObject.class);

        String mediaId = uploadObject.getId();

        ParseRequest parseRequest = ParseRequest
                .newBuilder()
                .setDomain(ParseRequest.Domain.CUSTOMER)
                .addMediaIds(mediaId)
                .build();
        ParseResponse parseResponse = apiClient.newTarget()
                .path("/api/parse")
                .request()
                .header(ApiClient.CUSTOM_SECRET_HEADER, apiClient.getCustomSecretHeader())
                .post(Entity.entity(parseRequest, MediaType.APPLICATION_JSON_TYPE))
                .readEntity(ParseResponse.class);
        assertTrue(parseResponse.getResultsCount() > 0);

        System.out.println(ObjectMapperProvider.get()
                .writerWithDefaultPrettyPrinter().writeValueAsString(parseResponse));
    }
}
