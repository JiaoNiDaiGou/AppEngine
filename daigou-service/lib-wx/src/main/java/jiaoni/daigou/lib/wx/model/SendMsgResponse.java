package jiaoni.daigou.lib.wx.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SendMsgResponse {
    @JsonProperty("BaseResponse")
    private BaseResponse baseResponse;
    @JsonProperty("MsgID")
    private String msgId;
    @JsonProperty("LocalID")
    private String localId;

    public BaseResponse getBaseResponse() {
        return baseResponse;
    }

    public void setBaseResponse(BaseResponse baseResponse) {
        this.baseResponse = baseResponse;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }
}
