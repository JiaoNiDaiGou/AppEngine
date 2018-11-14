package jiaoni.daigou.lib.wx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

public class InitResponse {
    @JsonProperty("BaseResponse")
    private BaseResponse baseResponse;

    // That is count for contact list.
    @JsonProperty("Count")
    private int count;

    @JsonProperty("ContactList")
    private List<Contact> contactList;

    @JsonProperty("SyncKey")
    private SyncKey syncKey;

    @JsonProperty("User")
    private Contact user;

    // A set of guys who currently I am chatting with.
    @JsonProperty("ChatSet")
    private String chatSet;

    @JsonProperty("Skey")
    private String skey;

    @JsonProperty("ClientVersion")
    private long clientVersion;

    @JsonProperty("SystemTime")
    private long systemTime;

    @JsonProperty("GrayScale")
    private int grayScale;

    @JsonProperty("InviteStartCount")
    private int inviteStartCount;

    // TODO:
    // I guess this is GONG ZHONG HAO
    @JsonProperty("MPSubscribeMsgCount")
    private int mpSubscribeMsgCount;

    @JsonProperty("MPSubscribeMsgList")
    private List<JsonNode> mpSubscribeMsgList;

    @JsonProperty("ClickReportInterval")
    private long clickReportInterval;

    public BaseResponse getBaseResponse() {
        return baseResponse;
    }

    public int getCount() {
        return count;
    }

    public List<Contact> getContactList() {
        return contactList;
    }

    public SyncKey getSyncKey() {
        return syncKey;
    }

    public Contact getUser() {
        return user;
    }

    public String getChatSet() {
        return chatSet;
    }

    public String getSkey() {
        return skey;
    }

    public long getClientVersion() {
        return clientVersion;
    }

    public long getSystemTime() {
        return systemTime;
    }

    public int getGrayScale() {
        return grayScale;
    }

    public int getInviteStartCount() {
        return inviteStartCount;
    }

    public int getMpSubscribeMsgCount() {
        return mpSubscribeMsgCount;
    }

    public List<JsonNode> getMpSubscribeMsgList() {
        return mpSubscribeMsgList;
    }

    public long getClickReportInterval() {
        return clickReportInterval;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
