package jiaoni.lib.teddy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

public class ShippingHistoryEntry {
    @JsonProperty
    private DateTime timestamp;

    @JsonProperty
    private String status;

    public static Builder builder() {
        return new Builder();
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

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    public static final class Builder {
        private DateTime timestamp;
        private String status;

        private Builder() {
        }

        public Builder withTimestamp(DateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withStatus(String status) {
            this.status = status;
            return this;
        }

        public ShippingHistoryEntry build() {
            ShippingHistoryEntry shippingHistoryEntry = new ShippingHistoryEntry();
            shippingHistoryEntry.timestamp = this.timestamp;
            shippingHistoryEntry.status = this.status;
            return shippingHistoryEntry;
        }
    }
}
