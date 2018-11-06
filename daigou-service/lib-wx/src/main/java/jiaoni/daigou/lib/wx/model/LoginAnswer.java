package jiaoni.daigou.lib.wx.model;

import jiaoni.daigou.lib.wx.Session;

public class LoginAnswer {
    private final LoginStatus status;
    private final Session session;

    public LoginAnswer(LoginStatus status, Session session) {
        this.status = status;
        this.session = session;
    }

    public LoginStatus getStatus() {
        return status;
    }

    public Session getSession() {
        return session;
    }
}
