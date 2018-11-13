package jiaoni.daigou.service.appengine.impls.wx;

import jiaoni.daigou.lib.wx.model.Contact;
import jiaoni.daigou.lib.wx.model.Message;
import jiaoni.daigou.lib.wx.model.MessageType;

/**
 * Wrapping WX {@link Message} with rich metadata.
 */
public class RichMessage {
    private final Message message;
    private final byte[] imageBytes;
    private final String imageMediaId;
    private final Contact fromContact;
    private final Contact toContact;
    private final boolean fromMyself;

    private RichMessage(Builder builder) {
        this.message = builder.message;
        this.imageBytes = builder.imageBytes;
        this.imageMediaId = builder.imageMediaId;
        this.fromContact = builder.fromContact;
        this.toContact = builder.toContact;
        this.fromMyself = builder.fromMyself;
    }

    public MessageType type() {
        return message.getMessageType();
    }

    public String getContent() {
        return message.getContent();
    }

    public Message getMessage() {
        return message;
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

    public Contact getToContact() {
        return toContact;
    }

    public boolean isFromMyself() {
        return fromMyself;
    }

    public static RichMessage.Builder builder() {
        return new RichMessage.Builder();
    }

    public static final class Builder {
        private Message message;
        private byte[] imageBytes;
        private String imageMediaId;
        private Contact fromContact;
        private Contact toContact;
        private boolean fromMyself;

        private Builder() {
        }

        public Builder withMessage(Message message) {
            this.message = message;
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

        public Builder withToCustomer(Contact toContact) {
            this.toContact = toContact;
            return this;
        }

        public Builder withFromMyself(boolean fromMyself) {
            this.fromContact = fromContact;
            return this;
        }

        public RichMessage build() {
            return new RichMessage(this);
        }
    }
}
