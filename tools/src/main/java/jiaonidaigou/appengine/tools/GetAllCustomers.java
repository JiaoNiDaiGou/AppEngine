package jiaonidaigou.appengine.tools;

import com.google.common.net.HttpHeaders;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.tools.remote.ApiClient;
import jiaonidaigou.appengine.wiremodel.entity.Customer;

import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static jiaonidaigou.appengine.tools.ResponseHandler.handle;

public class GetAllCustomers {
    public static void main(String[] args) {
        ApiClient client = new ApiClient(Env.PROD);
        Response response;

        response = client.newTarget("Direct upload")
                .path("/api/media/directUpload")
                .queryParam("ext", "txt")
                .queryParam("hasDownloadUrl", true)
                .request()
                .header(HttpHeaders.AUTHORIZATION, client.getGoogleAuthTokenBearerHeader())
                .post(Entity.entity("this is some content", MediaType.APPLICATION_OCTET_STREAM));

        List<Customer> customers = handle(response, new GenericType<List<Customer>>() {
        });

        System.out.println(customers);
    }
}
