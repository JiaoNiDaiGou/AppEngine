package jiaoni.daigou.tools;

import com.google.common.collect.ImmutableMap;
import jiaoni.common.model.Env;
import jiaoni.daigou.tools.remote.ApiClient;

import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

public class VerifyWxLogin {
    public static void main(String[] args) {
        final String code = "get_it_from_wx_mini_app";

        String ticketId = new ApiClient(Env.LOCAL)
                .newTarget()
                .path("api/wx/SongFan/login")
                .request()
                .post(Entity.entity(ImmutableMap.of("code", code), MediaType.APPLICATION_JSON))
                .readEntity(new GenericType<Map<String, String>>() {
                }).get("ticketId");

        System.out.println("ticketId: " + ticketId);
    }
}
