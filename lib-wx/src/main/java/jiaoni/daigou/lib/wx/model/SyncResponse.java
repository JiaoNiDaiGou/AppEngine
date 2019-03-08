package jiaoni.daigou.lib.wx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class SyncResponse {
    private BaseResponse baseResponse;

    @JsonProperty("AddMsgCount")
    private int addMsgCount;

    @JsonProperty("AddMsgList")
    private List<Message> addMsgList;

    @JsonProperty("ModContactCount")
    private int modContactCount;

    @JsonProperty("ModContactList")
    private List<JsonNode> modContactList;

    @JsonProperty("DelContactCount")
    private int delContactCount;

    @JsonProperty("DelContactList")
    private List<JsonNode> delContactList;

    @JsonProperty("ModChatRoomMemberCount")
    private int modChatRoomMemberCount;

    @JsonProperty("ContinueFlag")
    private int continueFlag;

    @JsonProperty("SyncKey")
    private SyncKey syncKey;

    @JsonProperty("SyncCheckKey")
    private SyncKey syncCheckKey;

    @JsonProperty("SKey")
    private String sKey;

    public BaseResponse getBaseResponse() {
        return baseResponse;
    }

    public void setBaseResponse(BaseResponse baseResponse) {
        this.baseResponse = baseResponse;
    }

    public int getAddMsgCount() {
        return addMsgCount;
    }

    public void setAddMsgCount(int addMsgCount) {
        this.addMsgCount = addMsgCount;
    }

    public List<Message> getAddMsgList() {
        return addMsgList;
    }

    public void setAddMsgList(List<Message> addMsgList) {
        this.addMsgList = addMsgList;
    }

    public int getModContactCount() {
        return modContactCount;
    }

    public void setModContactCount(int modContactCount) {
        this.modContactCount = modContactCount;
    }

    public List<JsonNode> getModContactList() {
        return modContactList;
    }

    public void setModContactList(List<JsonNode> modContactList) {
        this.modContactList = modContactList;
    }

    public int getDelContactCount() {
        return delContactCount;
    }

    public void setDelContactCount(int delContactCount) {
        this.delContactCount = delContactCount;
    }

    public List<JsonNode> getDelContactList() {
        return delContactList;
    }

    public void setDelContactList(List<JsonNode> delContactList) {
        this.delContactList = delContactList;
    }

    public int getModChatRoomMemberCount() {
        return modChatRoomMemberCount;
    }

    public void setModChatRoomMemberCount(int modChatRoomMemberCount) {
        this.modChatRoomMemberCount = modChatRoomMemberCount;
    }

    public int getContinueFlag() {
        return continueFlag;
    }

    public void setContinueFlag(int continueFlag) {
        this.continueFlag = continueFlag;
    }

    public SyncKey getSyncKey() {
        return syncKey;
    }

    public void setSyncKey(SyncKey syncKey) {
        this.syncKey = syncKey;
    }

    public SyncKey getSyncCheckKey() {
        return syncCheckKey;
    }

    public void setSyncCheckKey(SyncKey syncCheckKey) {
        this.syncCheckKey = syncCheckKey;
    }

    public String getsKey() {
        return sKey;
    }

    public void setsKey(String sKey) {
        this.sKey = sKey;
    }
}
