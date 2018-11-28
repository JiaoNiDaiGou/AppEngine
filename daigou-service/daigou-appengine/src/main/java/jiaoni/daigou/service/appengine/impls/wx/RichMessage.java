package jiaoni.daigou.service.appengine.impls.wx;

import jiaoni.daigou.lib.wx.model.Contact;
import jiaoni.daigou.lib.wx.model.Message;
import jiaoni.daigou.lib.wx.model.MessageType;

import java.util.Arrays;

/**
 * Wrapping WX {@link Message} with rich metadata.
 */
public class RichMessage {
    private final Message originalMessage;
    private final String textContent;
    private final byte[] imageBytes;
    private final String imageMediaId;
    private final boolean fromMyself;
    private final boolean groupMessage;

    /**
     * When fromMyself is true, this is myself's contact.
     * If it is a groupMessage, this is the group Contact.
     * Otherwise, it is the speaker contact
     */
    private final Contact fromContact;

    /**
     * If it is a group message, this is the contact of the speaker.
     * Otherwise, it is null.
     */
    private final Contact groupSpeakerContact;

    /**
     * When fromMyself is true, this is what I talk to.
     * - if in group chat, this is the group contact
     * - if 1:1 chat, this is the listener contact.
     * Otherwise, this is myself contact (someone is talking, i am the listener).
     */
    private final Contact toContact;

    private RichMessage(Builder builder) {
        originalMessage = builder.originalMessage;
        textContent = builder.textContent;
        imageBytes = builder.imageBytes;
        imageMediaId = builder.imageMediaId;
        fromMyself = builder.fromMyself;
        groupMessage = builder.groupMessage;
        fromContact = builder.fromContact;
        groupSpeakerContact = builder.groupSpeakerContact;
        toContact = builder.toContact;
    }

    public static Builder builder() {
        return new Builder();
    }

    public MessageType type() {
        return originalMessage.getMessageType();
    }

    public Message getOriginalMessage() {
        return originalMessage;
    }

    public String getTextContent() {
        return textContent;
    }

    public byte[] getImageBytes() {
        return Arrays.copyOf(imageBytes, imageBytes.length);
    }

    public String getImageMediaId() {
        return imageMediaId;
    }

    public boolean isFromMyself() {
        return fromMyself;
    }

    public boolean isGroupMessage() {
        return groupMessage;
    }

    public Contact getFromContact() {
        return fromContact;
    }

    public Contact getGroupSpeakerContact() {
        return groupSpeakerContact;
    }

    public Contact getToContact() {
        return toContact;
    }


    public static final class Builder {
        private Message originalMessage;
        private String textContent;
        private byte[] imageBytes;
        private String imageMediaId;
        private boolean fromMyself;
        private boolean groupMessage;
        private Contact fromContact;
        private Contact groupSpeakerContact;
        private Contact toContact;

        private Builder() {
        }

        public static Builder aRichMessage() {
            return new Builder();
        }

        public Builder withOriginalMessage(Message originalMessage) {
            this.originalMessage = originalMessage;
            return this;
        }

        public Builder withTextContent(String textContent) {
            this.textContent = textContent;
            return this;
        }

        public Builder withImageBytes(byte[] imageBytes) {
            this.imageBytes = Arrays.copyOf(imageBytes, imageBytes.length);
            return this;
        }

        public Builder withImageMediaId(String imageMediaId) {
            this.imageMediaId = imageMediaId;
            return this;
        }

        public Builder withFromMyself(boolean fromMyself) {
            this.fromMyself = fromMyself;
            return this;
        }

        public Builder withGroupMessage(boolean groupMessage) {
            this.groupMessage = groupMessage;
            return this;
        }

        public Builder withFromContact(Contact fromContact) {
            this.fromContact = fromContact;
            return this;
        }

        public Builder withGroupSpeakerContact(Contact groupSpeakerContact) {
            this.groupSpeakerContact = groupSpeakerContact;
            return this;
        }

        public Builder withToContact(Contact toContact) {
            this.toContact = toContact;
            return this;
        }

        public RichMessage build() {
            return new RichMessage(this);
        }
    }
}
