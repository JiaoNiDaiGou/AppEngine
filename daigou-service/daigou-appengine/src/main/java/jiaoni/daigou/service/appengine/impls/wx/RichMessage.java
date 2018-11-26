package jiaoni.daigou.service.appengine.impls.wx;

import jiaoni.daigou.lib.wx.model.Contact;
import jiaoni.daigou.lib.wx.model.Message;
import jiaoni.daigou.lib.wx.model.MessageType;

/**
 * Wrapping WX {@link Message} with rich metadata.
 */
public class RichMessage {
    private final Message originalMessage;
    private final String textContent;
    private final byte[] imageBytes;
    private final String imageMediaId;
    private final Contact fromContact;
    private final Contact speakerContactInGroup;
    private final Contact toContact;
    private final boolean fromMyself;
    private final boolean isGroupMessage;

    private RichMessage(Builder builder) {
        this.originalMessage = builder.originalMessage;
        this.textContent = builder.textContent;
        this.imageMediaId = builder.imageMediaId;
        this.imageBytes = builder.imageBytes;
        this.fromContact = builder.fromContact;
        this.toContact = builder.toContact;
        this.speakerContactInGroup = builder.speakerContactInGroup;
        this.fromMyself = builder.fromMyself;
        this.isGroupMessage = builder.isGroupMessage;
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
        return imageBytes;
    }

    public String getImageMediaId() {
        return imageMediaId;
    }

    public Contact getFromContact() {
        return fromContact;
    }

    public Contact getSpeakerContactInGroup() {
        return speakerContactInGroup;
    }

    public Contact getToContact() {
        return toContact;
    }

    public boolean isFromMyself() {
        return fromMyself;
    }

    public boolean isGroupMessage() {
        return isGroupMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Message originalMessage;
        private String textContent;
        private byte[] imageBytes;
        private String imageMediaId;
        private Contact fromContact;
        private Contact speakerContactInGroup;
        private Contact toContact;
        private boolean fromMyself;
        private boolean isGroupMessage;

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
            this.imageBytes = imageBytes;
            return this;
        }

        public Builder withImageMediaId(String imageMediaId) {
            this.imageMediaId = imageMediaId;
            return this;
        }

        public Builder withFromContact(Contact fromContact) {
            this.fromContact = fromContact;
            return this;
        }

        public Builder withSpeakerContactInGroup(Contact speakerContactInGroup) {
            this.speakerContactInGroup = speakerContactInGroup;
            return this;
        }

        public Builder withToContact(Contact toContact) {
            this.toContact = toContact;
            return this;
        }

        public Builder withFromMyself(boolean fromMyself) {
            this.fromMyself = fromMyself;
            return this;
        }

        public Builder withIsGroupMessage(final boolean isGroupMessage) {
            this.isGroupMessage = isGroupMessage;
            return this;
        }

        public RichMessage build() {
            return new RichMessage(this);
        }
    }
}
