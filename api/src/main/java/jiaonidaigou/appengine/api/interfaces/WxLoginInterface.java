package jiaonidaigou.appengine.api.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import jiaonidaigou.appengine.api.access.db.WxSessionDbClient;
import jiaonidaigou.appengine.api.auth.WxSessionTicket;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.utils.Secrets;
import jiaonidaigou.appengine.wiremodel.api.WxLoginRequest;
import jiaonidaigou.appengine.wiremodel.api.WxLoginResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.joda.time.DateTime;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static jiaonidaigou.appengine.api.auth.WxSessionTicket.DEFAULT_EXPIRATION_MILLIS;

@Path("/api/wx")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
public class WxLoginInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(WxLoginInterface.class);

    private final WxSessionDbClient dbClient;
    private final Map<String, WxAppInfo> wxAppInfos;
    private final HttpClient httpClient;

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
        this.httpClient = HttpClientBuilder.create().build();
    }

    @POST
    @Path("/{app}/login")
    public Response login(@PathParam("app") final String appName,
                          final WxLoginRequest loginRequest) {
        RequestValidator.validateAppName(appName);
        RequestValidator.validateNotNull(loginRequest);
        RequestValidator.validateNotBlank(loginRequest.getCode(), "code");

        WxAppInfo appInfo = wxAppInfos.get(appName);
        if (appInfo == null) {
            throw new NotFoundException();
        }
        WxSessionTicket ticket = getWxSessionTicket(appInfo, loginRequest.getCode());

        dbClient.put(ticket);

        WxLoginResponse response = WxLoginResponse.newBuilder().setTicketId(ticket.getTicketId()).build();
        return Response.ok(response).build();
    }

    private WxSessionTicket getWxSessionTicket(final WxAppInfo appInfo,
                                               final String code) {
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?js_code=%s&appid=%s&secret=%s&grant_type=authorization_code",
                code, appInfo.appId, appInfo.appSecret);
        LOGGER.info("Call Wx jscode2session: {}", url);

        HttpGet request = new HttpGet(url);

        JsonNode node;
        try {
            HttpResponse response = httpClient.execute(request);
            node = ObjectMapperProvider.get().readTree(response.getEntity().getContent());
        } catch (IOException e) {
            throw new ServiceUnavailableException("call Wx.jscode2session failed: " + e.getMessage());
        }

        if (node.has("errorcode")) {
            String error = node.get("errcode").asInt() + ":" + node.get("errmsg").asText();
            LOGGER.error("Wx jscode2session failed: {}", error);
            throw new ServiceUnavailableException(error);
        }

        WxSessionTicket ticket = WxSessionTicket.builder()
                .withTicketId(UUID.randomUUID().toString())
                .withExpirationTime(DateTime.now().plus(DEFAULT_EXPIRATION_MILLIS))
                .withOpenId(node.get("openid").asText())
                .withSessionKey(node.get("session_key").asText())
                .withUnionId(node.has("unionid") ? node.get("unionid").asText() : null)
                .build();
        LOGGER.info("Wx.jscode2session result: {}", ticket);
        return ticket;
    }
}
