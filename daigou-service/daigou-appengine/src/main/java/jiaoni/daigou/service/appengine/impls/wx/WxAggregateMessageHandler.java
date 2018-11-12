package jiaoni.daigou.service.appengine.impls.wx;

import com.google.common.collect.ImmutableList;
import jiaoni.daigou.lib.wx.Session;
import jiaoni.daigou.lib.wx.model.Message;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WxAggregateMessageHandler implements WxMessageHandler {
    private final List<WxMessageHandler> handlers;

    @Inject
    public WxAggregateMessageHandler(final WxCustomerParseMessageHandler customerParseMessageHandler) {
        handlers = ImmutableList.of(
                customerParseMessageHandler
        );
    }

    @Override
    public boolean handle(Session session, Message message) {
        for (WxMessageHandler handler : handlers) {
            boolean handled = handler.handle(session, message);
            if (handled) {
                return true;
            }
        }
        return false;
    }
}
