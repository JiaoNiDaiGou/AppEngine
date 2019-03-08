package jiaoni.daigou.service.appengine.impls.wx;

import com.google.common.collect.ImmutableList;
import jiaoni.daigou.lib.wx.Session;
import jiaoni.daigou.lib.wx.model.Contact;

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
    public boolean handle(Session session, RichMessage message) {
        if (canHandle(message)) {
            for (WxMessageHandler handler : handlers) {
                boolean handled = handler.handle(session, message);
                if (handled) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canHandle(final RichMessage message) {
        Contact toContact = message.getToContact();
        if (toContact == null
                || !toContact.getNickName().contains("代购奔小康")
                || message.getFromContact() == null
                || message.isFromMyself()) {
            return false;
        }
        return true;
    }
}
