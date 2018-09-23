package jiaonidaigou.appengine.api.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class WxSessionTicket {
    @JsonProperty("openid")
    private String openId;

    @JsonProperty("session_key")
    private String sessionKey;

    @JsonProperty("unionid")
    private String unionId;

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("openId", openId)
                .add("sessionKey", sessionKey)
                .add("unionId", unionId)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WxSessionTicket that = (WxSessionTicket) o;
        return Objects.equal(openId, that.openId) &&
                Objects.equal(sessionKey, that.sessionKey) &&
                Objects.equal(unionId, that.unionId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(openId, sessionKey, unionId);
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
        private String openId;
        private String sessionKey;
        private String unionId;

        private Builder() {
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
            wxSessionTicket.sessionKey = this.sessionKey;
            wxSessionTicket.openId = this.openId;
            wxSessionTicket.unionId = this.unionId;
            return wxSessionTicket;
        }
    }
}
