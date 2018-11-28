package jiaoni.daigou.service.appengine.impls.wx;

import jiaoni.daigou.lib.wx.Session;
import jiaoni.daigou.lib.wx.WxWebClient;
import jiaoni.daigou.lib.wx.model.Contact;
import jiaoni.daigou.lib.wx.model.Message;
import jiaoni.daigou.lib.wx.model.MessageType;
import org.apache.commons.lang3.StringUtils;

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

        RichMessage.Builder builder = RichMessage.builder()
                .withOriginalMessage(message)
                .withTextContent(message.getContent());

        // Image
        if (message.getMessageType() == MessageType.IMAGE) {
            byte[] bytes = wxWebClient.getMessageImage(session, message.getMsgId());
            builder.withImageBytes(bytes);
        }

        // From contact
        Contact fromContact = findContact(session, message.getFromUserName());
        builder.withFromContact(fromContact);
        if (fromContact != null) {
            if (fromContact.getUserName().equals(session.getMyself().getUserName())) {
                builder.withFromMyself(true);
            } else if (fromContact.getType() == Contact.Type.GROUP_CHAT_ACCOUNT) {
                builder.withGroupMessage(true);
                // For group message, the fromContact is the group name.
                // The contant is in format of 'speakerUserName:content';
                String speakerUsername = StringUtils.substringBefore(message.getContent(), ":");
                if (StringUtils.isNotBlank(speakerUsername)) {
                    builder.withGroupSpeakerContact(findContact(session, speakerUsername))
                            .withTextContent(StringUtils.substringAfter(message.getContent(), ":"));
                }
            }
        }

        // To contact
        builder.withToContact(findContact(session, message.getToUserName()));

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
