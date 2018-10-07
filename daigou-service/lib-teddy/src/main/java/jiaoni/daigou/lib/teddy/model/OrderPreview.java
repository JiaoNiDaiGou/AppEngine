package jiaoni.daigou.lib.teddy.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

public class OrderPreview {
    private long id;
    private String formattedId;
    private String rawStatus;
    private DateTime lastUpdatedTime;
    private String receiverName;
    private String productSummary;
    private String rawShippingStatus;
    private String trackingNumber;
    private double price;

    public double getPrice() {
        return price;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    private boolean idCardUploaded;

    public static Builder builder() {
        return new Builder();
    }

    public long getId() {
        return id;
    }

    public String getFormattedId() {
        return formattedId;
    }

    public String getRawStatus() {
        return rawStatus;
    }

    public DateTime getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getProductSummary() {
        return productSummary;
    }

    public String getRawShippingStatus() {
        return rawShippingStatus;
    }

    public boolean isIdCardUploaded() {
        return idCardUploaded;
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

    public static final class Builder {
        private long id;
        private String formattedId;
        private String rawStatus;
        private DateTime lastUpdatedTime;
        private String receiverName;
        private String productSummary;
        private String rawShippingStatus;
        private String trackingNumber;
        private double price;
        private boolean idCardUploaded;

        private Builder() {
        }

        public Builder withId(long id) {
            this.id = id;
            return this;
        }

        public Builder withFormattedId(String formattedId) {
            this.formattedId = formattedId;
            return this;
        }

        public Builder withRawStatus(String rawStatus) {
            this.rawStatus = rawStatus;
            return this;
        }

        public Builder withLastUpdatedTime(DateTime lastUpdatedTime) {
            this.lastUpdatedTime = lastUpdatedTime;
            return this;
        }

        public Builder withReceiverName(String receiverName) {
            this.receiverName = receiverName;
            return this;
        }

        public Builder withProductSummary(String productSummary) {
            this.productSummary = productSummary;
            return this;
        }

        public Builder withRawShippingStatus(String rawShippingStatus) {
            this.rawShippingStatus = rawShippingStatus;
            return this;
        }

        public Builder withTrackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
            return this;
        }

        public Builder withPrice(double price) {
            this.price = price;
            return this;
        }

        public Builder withIdCardUploaded(boolean idCardUploaded) {
            this.idCardUploaded = idCardUploaded;
            return this;
        }

        public OrderPreview build() {
            OrderPreview orderPreview = new OrderPreview();
            orderPreview.id = this.id;
            orderPreview.price = this.price;
            orderPreview.formattedId = this.formattedId;
            orderPreview.idCardUploaded = this.idCardUploaded;
            orderPreview.productSummary = this.productSummary;
            orderPreview.rawShippingStatus = this.rawShippingStatus;
            orderPreview.trackingNumber = this.trackingNumber;
            orderPreview.rawStatus = this.rawStatus;
            orderPreview.receiverName = this.receiverName;
            orderPreview.lastUpdatedTime = this.lastUpdatedTime;
            return orderPreview;
        }
    }
}
