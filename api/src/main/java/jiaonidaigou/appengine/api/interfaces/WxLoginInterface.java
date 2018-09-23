package jiaonidaigou.appengine.api.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import jiaonidaigou.appengine.api.access.db.WxSessionDbClient;
import jiaonidaigou.appengine.api.auth.WxSessionTicket;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.utils.Secrets;
import org.jvnet.hk2.annotations.Service;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/wx")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
public class WxLoginInterface {
    private final WxSessionDbClient dbClient;
    private final Map<String, WxAppInfo> wxAppInfos;

    private static class WxAppInfo {
        @JsonProperty
        String appId;
        @JsonProperty
        String appSecret;
    }

    @Inject
    public WxLoginInterface(final WxSessionDbClient dbClient) {
        this.dbClient = dbClient;
        this.wxAppInfos = Secrets.of("wx.appinfo.json")
                .getAsJson(new TypeReference<Map<String, WxAppInfo>>() {
                });
    }

    @POST
    @Path("/{app}/login")
    @Produces(MediaType.TEXT_PLAIN)
    public Response login(@PathParam("app") final String appName,
                          final String code) {
        RequestValidator.validateNotBlank(code, "code");
        RequestValidator.validateNotBlank(appName, "appName");

        WxAppInfo appInfo = wxAppInfos.get(appName);
        if (appInfo == null) {
            throw new NotFoundException();
        }
        WxSessionTicket ticket = getWxSessionTicket(appInfo, code);

        dbClient.put(ticket);

        return Response.ok(ticket.getTicketId()).build();
    }

    private static WxSessionTicket getWxSessionTicket(final WxAppInfo appInfo,
                                                      final String code) {
        try {
            URL url = new URL(String.format(
                    "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    appInfo.appId, appInfo.appSecret, code));

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(false);
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept", MediaType.APPLICATION_JSON);
            int status = connection.getResponseCode();
            if (status != 200) {
                try (InputStream inputStream = connection.getErrorStream()) {
                    JsonNode node = ObjectMapperProvider.get().readTree(inputStream);
                    String error = node.get("errcode").asInt() + ":" + node.get("errmsg").asText();
                    throw new ServiceUnavailableException(error);
                }
            }

            try (InputStream inputStream = connection.getInputStream()) {
                JsonNode node = ObjectMapperProvider.get().readTree(inputStream);
                return WxSessionTicket.builder()
                        .withTicketId(UUID.randomUUID().toString())
                        .withOpenId(node.get("openid").asText())
                        .withSessionKey(node.get("session_key").asText())
                        .withUnionId(node.has("unionid") ? node.get("unionid").asText() : null)
                        .build();
            }

        } catch (Exception e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
    }
}
