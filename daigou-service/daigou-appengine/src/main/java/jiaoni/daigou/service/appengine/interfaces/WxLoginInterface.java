package jiaoni.daigou.service.appengine.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import jiaoni.common.appengine.access.taskqueue.PubSubClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.appengine.auth.WxSessionDbClient;
import jiaoni.common.appengine.auth.WxSessionTicket;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.utils.Secrets;
import jiaoni.daigou.lib.wx.Session;
import jiaoni.daigou.lib.wx.WxWebClient;
import jiaoni.daigou.lib.wx.model.LoginAnswer;
import jiaoni.daigou.lib.wx.model.LoginStatus;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.service.appengine.impls.db.WxWebSessionDbClient;
import jiaoni.daigou.service.appengine.tasks.WxSyncTaskRunner;
import jiaoni.daigou.wiremodel.api.WxLoginRequest;
import jiaoni.daigou.wiremodel.api.WxLoginResponse;
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
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import static jiaoni.common.appengine.auth.WxSessionTicket.DEFAULT_EXPIRATION_MILLIS;

@Path("/api/wx")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
public class WxLoginInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(WxLoginInterface.class);

    // Mini Program
    private final WxSessionDbClient dbClient;
    private final Map<String, WxAppInfo> wxAppInfos;

    // Web WX
    private final WxWebClient wxWebClient;
    private final WxWebSessionDbClient wxWebSessionDbClient;

    private final HttpClient httpClient;
    private final PubSubClient pubSubClient;


    private static class WxAppInfo {
        @JsonProperty
        String appId;
        @JsonProperty
        String appSecret;
    }

    @Inject
    public WxLoginInterface(final WxSessionDbClient dbClient,
                            final WxWebClient wxWebClient,
                            final WxWebSessionDbClient wxWebSessionDbClient,
                            final PubSubClient pubSubClient) {
        this.dbClient = dbClient;
        this.wxAppInfos = Secrets.of("wx.appinfo.json")
                .getAsJson(new TypeReference<Map<String, WxAppInfo>>() {
                });
        this.httpClient = HttpClientBuilder.create().build();
        this.wxWebClient = wxWebClient;
        this.wxWebSessionDbClient = wxWebSessionDbClient;
        this.pubSubClient = pubSubClient;
    }

    @POST
    @Path("/login")
    public Response login(final WxLoginRequest loginRequest) {
        RequestValidator.validateNotNull(loginRequest);
        RequestValidator.validateNotBlank(loginRequest.getCode(), "code");

        WxAppInfo appInfo = wxAppInfos.get(AppEnvs.getServiceName());
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

    @GET
    @Path("/web/askLogin")
    public Response askLogin(@QueryParam("uuid") final String uuid) {
        RequestValidator.validateNotBlank(uuid);

        LoginAnswer loginAnswer = wxWebClient.askLogin(uuid);
        if (loginAnswer.getStatus() == LoginStatus.SUCCESS) {
            Session session = loginAnswer.getSession();
            wxWebClient.initialize(session);
            wxWebClient.statusNotify(session);
            session.setLastReplyTimestampNow();
            wxWebSessionDbClient.put(session);

            LOGGER.info("start WX web task");
            pubSubClient.submit(PubSubClient.QueueName.PROD_QUEUE,
                    TaskMessage.builder()
                            .withHandler(WxSyncTaskRunner.class)
                            .withPayloadJson(new WxSyncTaskRunner.WxSyncTicket(session.getSessionId(), 0))
                            .build());
        }

        return Response.ok(loginAnswer.getStatus()).build();
    }

    @GET
    @Path("/web/startLogin")
    public Response login() {
        String uuid = wxWebClient.fetchLoginUuid();
        return Response.ok(uuid).build();
    }

    @GET
    @Path("/web/fetchQR")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response fetchQr(@QueryParam("uuid") final String uuid) {
        RequestValidator.validateNotBlank(uuid);

        StreamingOutput stream = output -> wxWebClient.outputQrCode(uuid, output);

        return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header("content-disposition", "attachment; filename = qrcode.png")
                .build();
    }
}
