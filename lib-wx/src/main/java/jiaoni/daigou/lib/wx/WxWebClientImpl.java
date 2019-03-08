package jiaoni.daigou.lib.wx;

import com.google.common.collect.ImmutableMap;
import jiaoni.common.httpclient.BrowserClient;
import jiaoni.daigou.lib.wx.model.Contact;
import jiaoni.daigou.lib.wx.model.GetContactResponse;
import jiaoni.daigou.lib.wx.model.InitResponse;
import jiaoni.daigou.lib.wx.model.LoginAnswer;
import jiaoni.daigou.lib.wx.model.LoginStatus;
import jiaoni.daigou.lib.wx.model.Message;
import jiaoni.daigou.lib.wx.model.MessageType;
import jiaoni.daigou.lib.wx.model.SendMsgResponse;
import jiaoni.daigou.lib.wx.model.StatusNotifyResponse;
import jiaoni.daigou.lib.wx.model.StringResponse;
import jiaoni.daigou.lib.wx.model.SyncCheck;
import jiaoni.daigou.lib.wx.model.SyncResponse;
import jiaoni.daigou.lib.wx.model.WxException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static jiaoni.common.utils.LocalMeter.meterOff;
import static jiaoni.common.utils.LocalMeter.meterOn;
import static jiaoni.common.utils.Preconditions2.checkNotBlank;
import static jiaoni.daigou.lib.wx.WxUtils.generateClientMsgId;
import static jiaoni.daigou.lib.wx.WxUtils.generateRandomDeviceId;
import static jiaoni.daigou.lib.wx.WxUtils.nowMillisNegateToString;
import static jiaoni.daigou.lib.wx.WxUtils.nowMillisToString;
import static jiaoni.daigou.lib.wx.WxUtils.syncKeyToString;

public class WxWebClientImpl implements WxWebClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(WxWebClientImpl.class);
    private static final String LOGIN_URL_BASE = "https://login.web.wechat.com";
    private static final String LOGIN_URL_JSLOGIN = LOGIN_URL_BASE + "/jslogin";
    private static final String LOGIN_URL_QRCODE = LOGIN_URL_BASE + "/qrcode";
    private static final String LOGIN_URL_ASK_LOGIN = LOGIN_URL_BASE + "/cgi-bin/mmwebwx-bin/login";
    private static final String LANG_ZH_CN = "zh_CN";
    private static final int SUCCESS_CODE = 200;
    private final BrowserClient client;

    public WxWebClientImpl(final BrowserClient client) {
        this.client = client;
    }

    private static <T extends BrowserClient.DoHttp> Consumer<T> addCommonHeaders() {
        return t -> t
                .header(HttpHeaders.ACCEPT, "*/*")
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .header(HttpHeaders.REFERER, "https://web.wechat.com/")
                .header(HttpHeaders.ACCEPT_ENCODING, "gzip")
                .header(HttpHeaders.ACCEPT_ENCODING, "deflate")
                .header(HttpHeaders.ACCEPT_ENCODING, "br")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en;q=0.9")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN;q=0.8")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "zh;q=0.7")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-TW;q=0.6");
    }

    /**
     * Sample URL:
     * <pre>
     *     https://login.web.wechat.com/jslogin
     *      &appid=wx782c26e4c19acffb
     *      &redirect_uri=https%3A%2F%2Fweb.wechat.com%2Fcgi-bin%2Fmmwebwx-bin%2Fwebwxnewloginpage
     *      &fun=new
     *      &lang=zh_CN
     *      &_=1528597610927
     * </pre>
     * <p>
     * Sample response:
     * <pre>
     *     window.QRLogin.code = 200; window.QRLogin.uuid = "xxx"
     * </pre>
     */
    @Override
    public String fetchLoginUuid() {
        meterOn();

        StringResponse response = StringResponse.responseOf(client.doGet()
                .url(LOGIN_URL_JSLOGIN)
                .pathParam("appid", "wx782c26e4c19acffb")
                .pathParam("redirect_uri", "https://web.wechat.com/cgi-bin/mmwebwx-bin/webwxnewloginpage")
                .pathParam("fun", "new")
                .pathParam("lang", LANG_ZH_CN)
                .pathParam("_", nowMillisToString())
                .header(HttpHeaders.HOST, "login.web.wechat.com")
                .consume(addCommonHeaders())
//                .consume(addCommonCookies())
                .request()
                .callToString());

        LOGGER.info("Fetch UUID response: {}", response);
        int code = response.getAsInteger("window.QRLogin.code", -1);
        if (SUCCESS_CODE == code) {
            String uuid = response.getAsString("window.QRLogin.uuid");
            LOGGER.info("Get UUID {}", uuid);
            meterOff();
            return uuid;
        }
        meterOff();
        throw new WxException("failed to fetch login uuid. status=" + code);
    }

    @Override
    public void outputQrCode(final String loginUuid, final OutputStream outputStream) {
        checkNotBlank(loginUuid);
        checkNotNull(outputStream);

        meterOn();

        client.doGet()
                .url(LOGIN_URL_QRCODE + "/" + loginUuid)
                .header(HttpHeaders.REFERER, "https://web.wechat.com/")
                .request()
                .callToOutputSteam(outputStream);

        meterOff();
    }

    @Override
    public LoginAnswer askLogin(final String loginUuid) {
        checkNotBlank(loginUuid);

        meterOn();

        StringResponse response = StringResponse.responseOf(client.doGet()
                .url(LOGIN_URL_ASK_LOGIN)
                .pathParam("loginicon", "true")
                .pathParam("uuid", loginUuid)
                // 1 means haven't scanned yet
                // 0 means already scanned.
                .pathParam("tip", "0")
                .pathParam("r", nowMillisNegateToString())
                .pathParam("_", nowMillisToString())
                .header(HttpHeaders.HOST, "login.web.wechat.com")
                .consume(addCommonHeaders())
                .request()
                .callToString());

        LoginStatus status = LoginStatus.statusCodeOf(response.getAsInteger("window.code", -1));
        LOGGER.info("askLogin status {}", status);

        switch (status) {
            case SUCCESS: {
                LOGGER.info("askLogin: enter login.");
                LoginAnswer loginAnswer = login(response);
                meterOff();
                return loginAnswer;
            }
            case WAIT_CONFIRM_CODE: {
                LOGGER.info("askLogin: waiting for user to press confirm button");
                meterOff();
                return new LoginAnswer(LoginStatus.WAIT_CONFIRM_CODE, null);
            }
            case WAIT_SCAN_CODE: {
                LOGGER.info("askLogin: waiting for user to scan QR code");
                meterOff();
                return new LoginAnswer(LoginStatus.WAIT_SCAN_CODE, null);
            }
            case LOGIN_TIMEOUT: {
                LOGGER.info("askLogin: login timeout");
                meterOff();
                return new LoginAnswer(LoginStatus.LOGIN_TIMEOUT, null);
            }
            case ERROR:
            default: {
                LOGGER.error("askLogin: error.");
                meterOff();
                return new LoginAnswer(LoginStatus.ERROR, null);
            }
        }
    }

    /**
     * Initialize WX.
     * <p>
     * Example request
     * <pre>
     *     https://web.wechat.com/cgi-bin/mmwebwx-bin/webwxinit
     *          ?r=402993428
     *          &lang=zh_CN
     *          &pass_ticket=xxx
     * </pre>
     */
    @Override
    public void initialize(final Session session) {
        checkNotNull(session);
        checkState(session.isLoggedIn());

        LOGGER.info("initWx start");

        meterOn();

        InitResponse response = client.doPost()
                .url(session.getWebUrl() + "/webwxinit")
                .pathParam("r", nowMillisNegateToString())
                .pathParam("lang", LANG_ZH_CN)
                .pathParam("pass_ticket", session.getPassTicket())
                .jsonBody(builderBaseRequest(session).build())
                .request()
                .callToJson(InitResponse.class);

        if (response.getBaseResponse().hasError()) {
            LOGGER.error("init wx error: " + response.getBaseResponse() + ", init response: " + response);
            throw new WxException("failed to init").withWxBaseResponse(response.getBaseResponse());
        }
        session.setMyself(response.getUser());

        session.setSyncKey(response.getSyncKey());

        // NOTE
        // These are the partial contacts (group chat and public accounts)
        // call syncContacts() to sync full personal contacts.
        for (Contact contact : response.getContactList()) {
            System.out.println(contact);
        }
        session.updateContacts(response.getContactList());

        meterOff();
    }

    @Override
    public void statusNotify(Session session) {
        meterOn();

        client.doPost()
                .url(session.getWebUrl() + "/webwxstatusnotify")
                .pathParam("pass_ticket", session.getPassTicket())
                .jsonBody(builderBaseRequest(session)
                        .put("Code", 1) // TODO: what does it mean
                        .put("FromUserName", session.getMyself().getUserName())
                        .put("ToUserName", session.getMyself().getUserName())
                        .put("ClientMsgId", generateClientMsgId())
                        .build())
                .request()
                .callToJson(StatusNotifyResponse.class);

        meterOff();
    }

    @Override
    public SyncCheck syncCheck(final Session session) {
        checkNotNull(session);
        checkState(session.isLoggedIn());

        meterOn();

        SyncCheck syncCheck = SyncCheck.responseOf(client.doGet()
                .url(session.getWebpushUrl() + "/synccheck")
                .pathParam("_", nowMillisToString())
                .pathParam("skey", session.getSkey())
                .pathParam("sid", session.getWxsid())
                .pathParam("uin", session.getWxuin())
                .pathParam("deviceid", session.getDeviceId())
                .pathParam("synckey", syncKeyToString(
                        session.getSyncCheckKey() == null ? session.getSyncKey() : session.getSyncCheckKey()))
                .pathParam("r", nowMillisToString())
                .request()
                .callToString());

        // TODO:
        // Set as lastSyncCheck?
        session.setLastSyncCheckTimestamp(DateTime.now());

        meterOff();

        LOGGER.info("SyncCheck response {}", syncCheck);
        return syncCheck;
    }

    /**
     * Example URL:
     * <pre>
     *
     * </pre>
     * <p>
     * Example response:
     * <pre>
     * {
     *    "BaseResponse":{
     *       "Ret":0,
     *       "ErrMsg":""
     *    },
     *    "AddMsgCount":0,
     *    "AddMsgList":[
     *
     *    ],
     *    "ModContactCount":0,
     *    "ModContactList":[
     *
     *    ],
     *    "DelContactCount":0,
     *    "DelContactList":[
     *
     *    ],
     *    "ModChatRoomMemberCount":0,
     *    "ModChatRoomMemberList":[
     *
     *    ],
     *    "Profile":{
     *       "BitFlag":0,
     *       "UserName":{
     *          "Buff":""
     *       },
     *       "NickName":{
     *          "Buff":""
     *       },
     *       "BindUin":0,
     *       "BindEmail":{
     *          "Buff":""
     *       },
     *       "BindMobile":{
     *          "Buff":""
     *       },
     *       "Status":0,
     *       "Sex":0,
     *       "PersonalCard":0,
     *       "Alias":"",
     *       "HeadImgUpdateFlag":0,
     *       "HeadImgUrl":"",
     *       "Signature":""
     *    },
     *    "ContinueFlag":0,
     *    "SyncKey":{
     *       "Count":9,
     *       "List":[
     *          {
     *             "Key":1,
     *             "Val":669324500
     *          },
     *          {
     *             "Key":2,
     *             "Val":669324536
     *          }
     *       ]
     *    },
     *    "SKey":"",
     *    "SyncCheckKey":{
     *       "Count":9,
     *       "List":[
     *          {
     *             "Key":1,
     *             "Val":669324500
     *          },
     *          {
     *             "Key":2,
     *             "Val":669324536
     *          }
     *       ]
     *    }
     * }
     * </pre>
     */
    @Override
    public List<Message> sync(final Session session) {
        checkNotNull(session);
        checkState(session.isLoggedIn());

        meterOn();

        SyncResponse response = client.doPost()
                .url(session.getWebUrl() + "/webwxsync")
                .pathParam("sid", session.getWxsid())
                .pathParam("skey", session.getSkey())
                .pathParam("lang", LANG_ZH_CN)
                .pathParam("pass_ticket", session.getPassTicket())
                .jsonBody(builderBaseRequest(session)
                        .put("SyncKey", session.getSyncKey())
                        .put("rr", nowMillisNegateToString())
                        .build())
                .request()
                .callToJson(SyncResponse.class);

        session.setSyncKey(response.getSyncKey());
        session.setSyncCheckKey(response.getSyncCheckKey());

        // NOTE:
        // we only care messages here.

        List<Message> toReturn = new ArrayList<>();
        if (CollectionUtils.isEmpty(response.getAddMsgList())) {
            return toReturn;
        }

        for (Message message : response.getAddMsgList()) {
            switch (message.getMessageType()) {
                case TEXT:
                    if (StringUtils.isNotBlank(message.getContent())) {
                        toReturn.add(message);
                    }
                    break;
                default:
                    break;
            }
        }
        LOGGER.info("I should get {} messages. but I get {} messages in real.", response.getAddMsgCount(), toReturn.size());

        meterOff();
        return toReturn;
    }

    /**
     * Get contacts.
     * <p>
     * Sample URL:
     * <pre>
     *     https://web.wechat.com/cgi-bin/mmwebwx-bin/webwxgetcontact
     *          ?pass_ticket=xxx
     *          &r=1528861665463
     *          &seq=0
     *          &skey=@crypt_b71209ca_e1052b10b3d97fd76218b7e01047a854
     * </pre>
     */
    public void syncContacts(final Session session) {
        int seq = 0;
        do {
            seq = getContactsBySeq(session, seq);
        } while (seq != 0);
    }

    private int getContactsBySeq(final Session session, final int seq) {
        meterOn();
        LOGGER.info("Get contacts by seq {}", seq);

        GetContactResponse response = client.doGet()
                .url(session.getWebUrl() + "/webwxgetcontact")
                .pathParam("pass_ticket", session.getPassTicket())
                .pathParam("r", nowMillisToString())
                .pathParam("seq", seq)
                .pathParam("skey", session.getSkey())
                .request()
                .callToJson(GetContactResponse.class);

        if (response.getMemberList() != null) {
            session.updateContacts(response.getMemberList());
        }

        return response.getSeq();
    }

    /**
     * Request URL:
     * <pre>
     *     https://web.wechat.com/cgi-bin/mmwebwx-bin/webwxbatchgetcontact
     *          ?type=ex
     *          &r=1528861665583
     *          &&pass_ticket=xxx
     * </pre>
     * <p>
     * Example Body:
     * <pre>
     *     {
     *    "BaseRequest":{
     *       "Uin":2281435855,
     *       "Sid":"fhlhJVR/CQXQJjLM",
     *       "Skey":"@crypt_b71209ca_e1052b10b3d97fd76218b7e01047a854",
     *       "DeviceID":"e535260010807677"
     *    },
     *    "Count":3,
     *    "List":[
     *       {
     *          "UserName":"@@079023dfb26a96c067d1f4addc345837910bd12e19961d5ca35fddf724a9a2be",
     *          "ChatRoomId":""
     *       },
     *       {
     *          "UserName":"@@495f84d8b54940f761022ca34acd7f0b95bd54dd59f7fa3d93e626c3207b7b29",
     *          "ChatRoomId":""
     *       },
     *       {
     *          "UserName":"@@57268cd87ff142a23cfdd6f5d5a95059e1bbd92459f4924c5ad7d1f4fc19c0b2",
     *          "ChatRoomId":""
     *       }
     *    ]
     * }
     * </pre>
     */
    @Override
    public void batchGetContacts(final Session session, final List<String> chatUserNames) {
        checkNotNull(session);
        checkState(session.isLoggedIn());

        String response = client.doPost()
                .url(session.getWebUrl() + "/webwxbatchgetcontact")
                .pathParam("type", "ex")
                .pathParam("r", nowMillisToString())
                .pathParam("pass_ticket", session.getPassTicket())
                .jsonBody(builderBaseRequest(session)
                        .put("Count", chatUserNames.size())
                        .put("List", chatUserNames.stream().map(t -> ImmutableMap.of("UserName", t, "ChatRoomId", "")))
                        .build())
                .request()
                .callToString();

        System.out.println(response);
    }

    /**
     * Example request:
     * Request URL: https://web.wechat.com/cgi-bin/mmwebwx-bin/webwxstatreport?fun=new
     * Request Method: POST
     * <p>
     * Headers:
     * Accept: application/json, text/plain, * / *
     * Accept-Encoding: gzip, deflate, br
     * Accept-Language: en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7,zh-TW;q=0.6
     * Connection: keep-alive
     * Content-Length: 1010
     * Content-Type: application/json;charset=UTF-8
     * Cookie: MM_WX_NOTIFY_STATE=1; MM_WX_SOUND_STATE=1; _ga=GA1.2.404212324.1542000888; webwxuvid=6ca396f5e791f0af578c45e5a3e34bdc007f359efa8d6e1b9efe7358d906cc88f38e95293049d12b78b53e2ba17cc41c; webwx_auth_ticket=CIsBENvxlJEBGoABljMwxnZZLVHUvoik1T+mh0I7H8B5TpR0FZIctfZMe8bI9mFgNaLvLEPaSXCoiM1e+HrV8fFQ34DqtAr8jvU/gxi8a6yP/zyygTwZcnros2Yl4fGJz/G36WDNQZQP5NU9XqU3aidOdrWQcsOAWluwRKRv5xoP8tjKzJvgjOeQdLE=; mm_lang=en
     * Host: web.wechat.com
     * Origin: https://web.wechat.com
     * Referer: https://web.wechat.com/
     * User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36
     * <p>
     * Body:
     * <pre>
     *
     * {
     *    "BaseRequest":{
     *       "Uin":"",
     *       "Sid":"",
     *       "DeviceID":"e639596671824548"
     *    },
     *    "Count":2,
     *    "List":[
     *       {
     *          "Type":1,
     *          "Text":"{\"type\":\"[app-runtime]\",\"data\":{\"unload\":{\"listenerCount\":117,\"watchersCount\":114,\"scopesCount\":31}}}"
     *       },
     *       {
     *          "Type":1,
     *          "Text":"{\"type\":\"[app-timing]\",\"data\":{\"appTiming\":{\"qrcodeStart\":1542587176878,\"qrcodeEnd\":1542587177822},\"pageTiming\":{\"navigationStart\":1542587175540,\"unloadEventStart\":0,\"unloadEventEnd\":0,\"redirectStart\":0,\"redirectEnd\":0,\"fetchStart\":1542587175545,\"domainLookupStart\":1542587175545,\"domainLookupEnd\":1542587175545,\"connectStart\":1542587175545,\"connectEnd\":1542587175545,\"secureConnectionStart\":0,\"requestStart\":1542587175548,\"responseStart\":1542587175549,\"responseEnd\":1542587175564,\"domLoading\":1542587175565,\"domInteractive\":1542587176558,\"domContentLoadedEventStart\":1542587176559,\"domContentLoadedEventEnd\":1542587176572,\"domComplete\":1542587202159,\"loadEventStart\":1542587202159,\"loadEventEnd\":1542587202165}}}"
     *       }
     *    ]
     * }
     *
     * </pre>
     */
    @Override
    public void statReport() {
    }

    @Override
    public byte[] getMessageImage(Session session, String messageId) {
        return getMessageMediaBytes("/webwxgetmsgimg", session, messageId);
    }

    @Override
    public byte[] getMessageVideo(Session session, String messageId) {
        return getMessageMediaBytes("/webwxgetvideo", session, messageId);
    }

    @Override
    public byte[] getMessageVoice(Session session, String messageId) {
        return getMessageMediaBytes("/webwxgetvoice", session, messageId);
    }

    @Override
    public void sendReply(Session session, WxReply reply) {
        checkNotNull(session);
        checkState(session.isLoggedIn());
        checkNotNull(reply);

        for (WxReply.ReplyAction replyAction : reply.getActions()) {
            if (replyAction instanceof WxReply.TextReply) {
                sendTextReply(session, (WxReply.TextReply) replyAction);
            } else {
                LOGGER.error("unexpected replay action " + replyAction.getClass().getSimpleName());
            }
        }
    }

    /**
     * Send text message.
     * <p>
     * Sample URL:
     * <pre>
     *     https://web.wechat.com/cgi-bin/mmwebwx-bin/webwxsendmsg
     *          ?pass_ticket=xxxx
     * </pre>
     * <p>
     * Payload:
     * <pre>
     *     {
     *    "BaseRequest":{
     *       "Uin":2281435855,
     *       "Sid":"fhlhJVR/CQXQJjLM",
     *       "Skey":"@crypt_b71209ca_e1052b10b3d97fd76218b7e01047a854",
     *       "DeviceID":"e346470863854256"
     *    },
     *    "Msg":{
     *       "Type":1,
     *       "Content":"hellowr",
     *       "FromUserName":"@6b0836e3bf03671f8d964dfbe68bdfa9ec9084632cc511d8191f512f7b88ad79",
     *       "ToUserName":"@3842236004681b0c1a175aa118f2ec97",
     *       "LocalID":"15288638822350996",
     *       "ClientMsgId":"15288638822350996"
     *    },
     *    "Scene":0
     * }
     * </pre>
     */
    private boolean sendTextReply(final Session session, final WxReply.TextReply reply) {
        meterOn();

        final String clientMsgId = generateClientMsgId();
        SendMsgResponse response = client.doPost()
                .url(session.getWebUrl() + "/webwxsendmsg")
                .pathParam("pass_ticket", session.getPassTicket())
                .jsonBody(builderBaseRequest(session)
                        .put("Scene", 0)
                        .put("Msg", ImmutableMap.<String, Object>builder()
                                .put("Type", MessageType.TEXT.type())
                                .put("Content", reply.getText())
                                .put("FromUserName", session.getMyself().getUserName())
                                .put("ToUserName", reply.getToUserName())
                                .put("LocalID", clientMsgId)
                                .put("ClientMsgId", clientMsgId)
                                .build())
                        .build())
                .request()
                .callToJson(SendMsgResponse.class);
        LOGGER.info("send text message response: {}", response);

        boolean success = true;
        if (response.getBaseResponse().hasError()) {
            LOGGER.error("failed to send text message. {}", response);
            success = false;
        }

        LOGGER.info("Send message success. MsgId:{}", response.getMsgId());
        meterOff();
        return success;
    }

    private byte[] getMessageMediaBytes(String path, Session session, String messageId) {
        checkState(session.isLoggedIn());
        checkNotBlank(messageId);

        meterOn();

        // set type=slave to get a minimum version.
        byte[] toReturn = client.doGet()
                .url(session.getWebUrl() + path)
                .pathParam("MsgID", messageId)
                .pathParam("skey", session.getSkey())
                .request()
                .callToBytes();

        meterOff();

        return toReturn;
    }

    private LoginAnswer login(final StringResponse askLoginResponse) {
        meterOn();

        String redirectUri = askLoginResponse.getAsString("window.redirect_uri");
        if (StringUtils.isBlank(redirectUri)) {
            LOGGER.error("askLogin: success. but no redirect uri. response: {}", askLoginResponse);
            meterOff();
            return new LoginAnswer(LoginStatus.ERROR, null);
        }

        Session session = new Session("wx." + UUID.randomUUID().toString(), DateTime.now());

        // redirect_uri looks like
        //      https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxnewloginpage?
        //          ticket=xxx&
        //          uuid=xxx&
        //          lang=xxx&
        //          scan=xxx
        //
        // Then the web bin base URL: https://wx.qq.com/cgi-bin/mmwebwx-bin/
        // File url: https://file.wx.qq.com/cgi-bin/mmwebwx-bin/
        // Webpush url: https://webpush.wx.qq.com/cgi-bin/mmwebwx-bin/
        String urlBase = StringUtils.substringBetween(redirectUri, "https://", "/");
        session.setWebUrl("https://" + urlBase + "/cgi-bin/mmwebwx-bin");
        session.setWebpushUrl("https://webpush." + urlBase + "/cgi-bin/mmwebwx-bin");

        // Set a random 15 digit as DeviceId.
        session.setDeviceId(generateRandomDeviceId());

        // Call redirect: webwxnewloginpage
        // Sample URL:
        //      https://web.wechat.com/cgi-bin/mmwebwx-bin/webwxnewloginpage
        //          ?ticket=Aruxrv94WDYiCvas3eiBF27s@qrticket_0
        //          &uuid=wYSKXptAGQ==
        //          &lang=en_US
        //          &scan=1528609363
        //          &fun=new
        //          &version=v2
        //          &lang=zh_CN

        String response = client.doGet()
                .url(redirectUri)
                .header(HttpHeaders.HOST, "web.wechat.com")
                .consume(addCommonHeaders())
//                .consume(addCommonCookies())
                .request()
                .callToString();

        // 如果登录被禁止时，则登录返回的message内容不为空，下面代码则判断登录内容是否为空，不为空则退出程序
        String bannedMessage = StringUtils.substringBetween(response, "<message>", "</message>");
        if (StringUtils.isNotBlank(bannedMessage)) {
            LOGGER.warn("postLogin: login is banned. {}", bannedMessage);
            meterOff();
            return new LoginAnswer(LoginStatus.BANNED, null);
        }

        Document document = Jsoup.parse(response);
        if (document == null) {
            LOGGER.error("postLogin: Failed to parse response as XML.");
            meterOff();
            return new LoginAnswer(LoginStatus.ERROR, null);
        }

        // The response looks like:
        //  <error>
        //    <ret>0</ret>
        //    <message />
        //    <skey>@crypt_b71209ca_0060a6ba27e3dd92ffad0562042b39bf</skey>
        //    <wxsid>eUJ1idmqPGKEYgtj</wxsid>
        //    <wxuin>2281435855</wxuin>
        //    <pass_ticket>Jx6vDgnAHuz5PutFBAuNabhoc8%2FPSBWomD%2FpyZ5nAzFhqbjYYu%2F1neLbIjiOfHnA</pass_ticket>
        //    <isgrayscale>1</isgrayscale>
        // </error>
        session.setSkey(document.getElementsByTag("skey").text());
        session.setWxsid(document.getElementsByTag("wxsid").text());
        session.setWxuin(document.getElementsByTag("wxuin").text());
        session.setPassTicket(document.getElementsByTag("pass_ticket").text());
        session.setLoggedIn(true);

        meterOff();
        return new LoginAnswer(LoginStatus.SUCCESS, session);
    }

    /**
     * Build 'BaseRequest' used in POST requests to Wx.
     */
    private static ImmutableMap.Builder<String, Object> builderBaseRequest(final Session session) {
        return ImmutableMap
                .<String, Object>builder()
                .put("BaseRequest",
                        ImmutableMap.of(
                                "Uin", session.getWxuin(),
                                "Sid", session.getWxsid(),
                                "Skey", session.getSkey(),
                                "DeviceID", session.getDeviceId()
                        ));
    }
}
