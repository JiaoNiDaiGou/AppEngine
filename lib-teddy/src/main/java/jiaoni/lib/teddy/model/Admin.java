package jiaoni.lib.teddy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Admin {
    @JsonProperty
    private String userId;

    @JsonProperty
    private String loginUsername;

    @JsonProperty
    private String loginPassword;

    @JsonProperty
    private String senderName;

    @JsonProperty
    private String senderPhone;

    @JsonProperty
    private String senderAddress;

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getUserId() {
        return userId;
    }

    public String getLoginUsername() {
        return loginUsername;
    }

    public String getLoginPassword() {
        return loginPassword;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public static final class Builder {
        private String userId;
        private String loginUsername;
        private String loginPassword;
        private String senderName;
        private String senderPhone;
        private String senderAddress;

        private Builder() {
        }

        public Builder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder withLoginUsername(String loginUsername) {
            this.loginUsername = loginUsername;
            return this;
        }

        public Builder withLoginPassword(String loginPassword) {
            this.loginPassword = loginPassword;
            return this;
        }

        public Builder withSenderName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public Builder withSenderPhone(String senderPhone) {
            this.senderPhone = senderPhone;
            return this;
        }

        public Builder withSenderAddress(String senderAddress) {
            this.senderAddress = senderAddress;
            return this;
        }

        public Admin build() {
            Admin admin = new Admin();
            admin.senderPhone = this.senderPhone;
            admin.loginPassword = this.loginPassword;
            admin.senderAddress = this.senderAddress;
            admin.senderName = this.senderName;
            admin.loginUsername = this.loginUsername;
            admin.userId = this.userId;
            return admin;
        }
    }
}
