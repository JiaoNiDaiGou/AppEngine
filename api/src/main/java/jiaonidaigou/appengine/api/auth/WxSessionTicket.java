package jiaonidaigou.appengine.api.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.Duration;

public class WxSessionTicket {
    public static final long DEFAULT_EXPIRATION_MILLIS = Duration.standardHours(1).getMillis();

    @JsonProperty
    private String ticketId;

    @JsonProperty
    private String openId;

    @JsonProperty
    private String sessionKey;

    @JsonProperty
    private String unionId;

    @JsonProperty
    private DateTime expirationTime;

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .withTicketId(ticketId)
                .withOpenId(openId)
                .withSessionKey(sessionKey)
                .withUnionId(unionId)
                .withExpirationTime(expirationTime);
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

    public DateTime getExpirationTime() {
        return expirationTime;
    }

    public static final class Builder {
        private String ticketId;
        private String openId;
        private String sessionKey;
        private String unionId;
        private DateTime expirationTime;

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

        public Builder withExpirationTime(DateTime expirationTime) {
            this.expirationTime = expirationTime;
            return this;
        }

        public WxSessionTicket build() {
            WxSessionTicket wxSessionTicket = new WxSessionTicket();
            wxSessionTicket.unionId = this.unionId;
            wxSessionTicket.ticketId = this.ticketId;
            wxSessionTicket.sessionKey = this.sessionKey;
            wxSessionTicket.openId = this.openId;
            wxSessionTicket.expirationTime = this.expirationTime;
            return wxSessionTicket;
        }
    }
}
