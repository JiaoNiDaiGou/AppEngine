package jiaoni.daigou.lib.wx.model;

import java.util.Arrays;

public enum MessageType {
    TEXT(1),
    IMAGE(3),
    VOICE(34),
    VERIFY_FRIEND(37),
    POSSIBLE_FRIEND(40),
    SHARE_NAMECARD(42),
    VIDEO_CALL(43),
    ANIMOJI(47),
    LOCATION_MESSAGE(48),
    SHARE_LINK(49),
    VOIP_MESSAGE(50),
    WX_INIT(51),
    VOIP_NOTIFY(52),
    VOIP_INVITE(53),
    SHORT_VIDEO(62),
    SYS_NOTICE(9999),
    SYSTEM_MESSAGE(10000), //系統消息
    DISGARD_MESSAGE(10002), //撤回消息
    UNKNOWN(-1);

    int type;

    MessageType(final int type) {
        this.type = type;
    }

    MessageType() {
        this.type = -1;
    }

    public static MessageType typeOf(final int type) {
        return Arrays.stream(MessageType.values())
                .filter(t -> t.type == type)
                .findFirst()
                .orElse(UNKNOWN);
    }

    public int type() {
        return type;
    }
}
