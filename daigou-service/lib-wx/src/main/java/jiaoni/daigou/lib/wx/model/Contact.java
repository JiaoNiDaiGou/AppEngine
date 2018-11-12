package jiaoni.daigou.lib.wx.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Base64;
import java.util.Set;

/**
 * Represent contact, and chat group.
 */
public class Contact {
    private static final Set<String> RESERVED_USER_NAMES = ImmutableSet.of(
            "filehelper",
            "newsapp",
            "fmessage",
            "weibo",
            "qqmail",
            "fmessage",
            "tmessage",
            "qmessage",
            "qqsync",
            "floatbottle",
            "lbsapp",
            "shakeapp",
            "medianote",
            "qqfriend",
            "readerapp",
            "blogapp",
            "facebookapp",
            "masssendapp",
            "meishiapp",
            "feedsapp",
            "voip",
            "blogappweixin",
            "weixin",
            "brandsessionholder",
            "weixinreminder",
            "officialaccounts",
            "notification_messages",
            "wxitil",
            "userexperience_alarm",
            "notification_messages"
    );

    public enum Type {
        // username starts with @. E.g. @xxx
        PERSONAL_ACCOUNT,

        // username starts with @@. E.g. @@xxx
        GROUP_CHAT_ACCOUNT,

        // username starts with @. But VerifyFlag & 8 != 0.
        // normal public account: verify flag = 8.
        // WX public account: 24
        // WX official group: 56
        PUBLIC_ACCOUNT,

        RESERVED_SYSTEM_ACCOUNT
    }

    // Temp user name
    @JsonProperty("UserName")
    private String userName;

    // Nick name.
    @JsonProperty("NickName")
    private String nickName;

    @JsonProperty("ContactFlag")
    private int contactFlag;

    @JsonProperty("VerifyFlag")
    private int verifyFlag;

    @JsonProperty("PYQuanPin")
    private String pyQuanPin;

    @JsonProperty("ChatRoomId")
    private int chatRoomId;

    @JsonProperty("EncryChatRoomId")
    private String encryChatRoomId;

    // 0 means not the owner of current user.
    @JsonProperty("IsOwner")
    private int isOwner;

    @JsonIgnore
    private String internalId;

    @JsonIgnore
    private Type type;

    @JsonIgnore
    public Type getType() {
        if (type == null) {
            String username = getUserName();
            if (RESERVED_USER_NAMES.contains(username)) {
                type = Type.RESERVED_SYSTEM_ACCOUNT;
            } else if (StringUtils.startsWith(username, "@@")) {
                type = Type.GROUP_CHAT_ACCOUNT;
            } else if (StringUtils.startsWith(username, "@")) {
                int verifyFlag = getVerifyFlag();
                if (verifyFlag != 0 && verifyFlag % 8 == 0) {
                    return Type.PUBLIC_ACCOUNT;
                }
                type = Type.PERSONAL_ACCOUNT;
            } else {
                // unknown type
                type = Type.PERSONAL_ACCOUNT;
            }
        }
        return type;
    }

    @JsonIgnore
    public String getInternalId() {
        if (internalId == null) {
            if (StringUtils.isBlank(pyQuanPin)) {
                return null;
            }
            String name = getType() + "|" + pyQuanPin + "|" + nickName;
            internalId = Base64.getEncoder().encodeToString(name.getBytes(Charsets.UTF_8));
        }
        return internalId;
    }

    public String getUserName() {
        return userName;
    }

    public String getNickName() {
        return nickName;
    }

    public int getContactFlag() {
        return contactFlag;
    }

    public int getVerifyFlag() {
        return verifyFlag;
    }

    public String getPyQuanPin() {
        return pyQuanPin;
    }

    public int getChatRoomId() {
        return chatRoomId;
    }

    public String getEncryChatRoomId() {
        return encryChatRoomId;
    }

    public int getIsOwner() {
        return isOwner;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
