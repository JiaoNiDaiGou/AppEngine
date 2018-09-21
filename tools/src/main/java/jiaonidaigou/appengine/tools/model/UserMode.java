package jiaonidaigou.appengine.tools.model;

public enum UserMode {
    JIAO("1437",
            "xiaoxiao9143@gmail.com",
            "JAY2020405630",
            "娇妮",
            "2138801085",
            "sammamish"),
    HACK("1899",
            "songfan.rfu@gmail.com",
            "furuijie",
            "Song Fan",
            "2137171237",
            "Seattle");
    private final String userId;
    private final String loginUsername;
    private final String loginPassword;
    private final String senderName;
    private final String senderPhone;
    private final String senderAddress;

    UserMode(final String userId,
             final String loginUsername,
             final String loginPassword,
             final String senderName,
             final String senderPhone,
             final String senderAddress) {
        this.userId = userId;
        this.loginUsername = loginUsername;
        this.loginPassword = loginPassword;
        this.senderName = senderName;
        this.senderPhone = senderPhone;
        this.senderAddress = senderAddress;
    }

    public String getUserId() {
        return userId;
    }

    public String getLoginUsername() {
        return loginUsername;
    }

    public String getLoginPassword() {
        return loginPassword;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public String getSenderAddress() {
        return senderAddress;
    }
}
