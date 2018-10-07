package jiaoni.lib.teddy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Receiver {
    @JsonProperty
    private String userId;

    @JsonProperty
    private String name;

    @JsonProperty
    private String phone;

    @JsonProperty
    private String addressRegion;

    @JsonProperty
    private String addressCity;

    @JsonProperty
    private String addressZone;

    @JsonProperty
    private String address;

    @JsonProperty
    private String idCardNumber;

    public static Builder builder() {
        return new Builder();
    }

    public String getIdCardNumber() {
        return idCardNumber;
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

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddressRegion() {
        return addressRegion;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public String getAddressZone() {
        return addressZone;
    }

    public String getAddress() {
        return address;
    }

    public static final class Builder {
        private String userId;
        private String name;
        private String phone;
        private String addressRegion;
        private String addressCity;
        private String addressZone;
        private String address;
        private String idCardNumber;

        private Builder() {
        }

        public Builder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder withAddressRegion(String addressRegion) {
            this.addressRegion = addressRegion;
            return this;
        }

        public Builder withAddressCity(String addressCity) {
            this.addressCity = addressCity;
            return this;
        }

        public Builder withAddressZone(String addressZone) {
            this.addressZone = addressZone;
            return this;
        }

        public Builder withAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder withIdCardNumber(String idCardNumber) {
            this.idCardNumber = idCardNumber;
            return this;
        }

        public Receiver build() {
            Receiver receiver = new Receiver();
            receiver.idCardNumber = this.idCardNumber;
            receiver.userId = this.userId;
            receiver.phone = this.phone;
            receiver.addressZone = this.addressZone;
            receiver.address = this.address;
            receiver.name = this.name;
            receiver.addressCity = this.addressCity;
            receiver.addressRegion = this.addressRegion;
            return receiver;
        }
    }
}
