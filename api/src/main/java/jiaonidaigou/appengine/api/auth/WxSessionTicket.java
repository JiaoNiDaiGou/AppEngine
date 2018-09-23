package jiaonidaigou.appengine.api.auth;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class WxSessionTicket {
    private String ticketId;
    private String openId;
    private String sessionKey;
    private String unionId;

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .withTicketId(ticketId)
                .withOpenId(openId)
                .withSessionKey(sessionKey)
                .withUnionId(unionId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getOpenId() {
        return openId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public String getUnionId() {
        return unionId;
    }

    public static final class Builder {
        private String ticketId;
        private String openId;
        private String sessionKey;
        private String unionId;

        private Builder() {
        }

        public Builder withTicketId(String ticketId) {
            this.ticketId = ticketId;
            return this;
        }

        public Builder withOpenId(String openId) {
            this.openId = openId;
            return this;
        }

        public Builder withSessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
            return this;
        }

        public Builder withUnionId(String unionId) {
            this.unionId = unionId;
            return this;
        }

        public WxSessionTicket build() {
            WxSessionTicket wxSessionTicket = new WxSessionTicket();
            wxSessionTicket.unionId = this.unionId;
            wxSessionTicket.ticketId = this.ticketId;
            wxSessionTicket.sessionKey = this.sessionKey;
            wxSessionTicket.openId = this.openId;
            return wxSessionTicket;
        }
    }
}
