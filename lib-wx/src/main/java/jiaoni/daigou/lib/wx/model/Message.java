package jiaoni.daigou.lib.wx.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {
    @JsonProperty("MsgId")
    private String msgId;

    @JsonProperty("FromUserName")
    private String fromUserName;

    @JsonProperty("ToUserName")
    private String toUserName;

    @JsonProperty("MsgType")
    private int msgType;

    @JsonProperty("Content")
    private String content;

    @JsonProperty("Status")
    private int status;

    @JsonProperty("CreateTime")
    private long createTime;

    @JsonIgnore
    private MessageType messageType;

    public String getMsgId() {
        return msgId;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public String getToUserName() {
        return toUserName;
    }

    public String getContent() {
        return content;
    }

    public int getStatus() {
        return status;
    }

    public long getCreateTime() {
        return createTime;
    }

    @JsonIgnore
    public MessageType getMessageType() {
        if (messageType == null) {
            messageType = MessageType.typeOf(this.msgType);
        }
        return messageType;
    }
}
