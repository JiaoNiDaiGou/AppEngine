package jiaoni.daigou.lib.wx.model;

import java.util.Arrays;

public enum LoginStatus {
    WAIT_SCAN_CODE(400),
    WAIT_CONFIRM_CODE(201),
    SUCCESS(200),
    LOGIN_TIMEOUT(408),
    BANNED(-2),
    ERROR(-1);

    private int statusCode;

    LoginStatus(final int statusCode) {
        this.statusCode = statusCode;
    }

    public static LoginStatus statusCodeOf(int status) {
        return Arrays.stream(LoginStatus.values())
                .filter(t -> t.statusCode == status)
                .findFirst()
                .orElse(null);
    }
}
