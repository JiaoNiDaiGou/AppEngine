package jiaoni.daigou.service.appengine.impls.wx;

import jiaoni.daigou.lib.wx.Session;
import jiaoni.daigou.lib.wx.model.Message;

public interface WxMessageHandler {

    /**
     * Handle WX message.
     *
     * @param session WX session.
     * @param message Wx message.
     * @return true if it has any reply.
     */
    boolean handle(final Session session, final Message message);
}
