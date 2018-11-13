package jiaoni.daigou.service.appengine.impls.wx;

import jiaoni.daigou.lib.wx.Session;
import jiaoni.daigou.lib.wx.WxWebClient;
import jiaoni.daigou.lib.wx.model.Contact;
import jiaoni.daigou.lib.wx.model.Message;
import jiaoni.daigou.lib.wx.model.MessageType;

import javax.inject.Inject;
import javax.inject.Singleton;

import static jiaoni.common.utils.CollectionUtils2.firstNotNull;

@Singleton
public class WxMessageEnricher {
    private WxWebClient wxWebClient;

    @Inject
    public WxMessageEnricher(final WxWebClient wxWebClient) {
        this.wxWebClient = wxWebClient;
    }

    public RichMessage enrich(final Session session, final Message message) {
        if (message == null) {
            return null;
        }

        RichMessage.Builder builder = RichMessage.builder();

        // Image
        if (message.getMessageType() == MessageType.IMAGE) {
            byte[] bytes = wxWebClient.getMessageImage(session, message.getMsgId());
            builder.withImageBytes(bytes);
        }

        // From & To contact
        Contact fromContact = findContact(session, message.getFromUserName());
        builder.withFromContact(fromContact)
                .withFromMyself(fromContact != null && fromContact.getUserName().equals(session.getMyself().getUserName()))
                .withToCustomer(findContact(session, message.getToUserName()));

        return builder.build();
    }

    private Contact findContact(final Session session, final String userName) {
        return firstNotNull(
                session.getMyself().getUserName().equals(userName) ? session.getMyself() : null,
                session.getPersonalAccounts().get(userName),
                session.getGroupChatAccounts().get(userName)
        );
    }
}
