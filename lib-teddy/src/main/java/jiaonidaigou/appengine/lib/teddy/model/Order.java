package jiaonidaigou.appengine.lib.teddy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class Order {
    /**
     * Order ID in long. It looks like 103263.
     * It is the canonical identifier for an order.
     */
    @JsonProperty
    private long id;
    /**
     * Formatted ID. It looks like 'RB100093036' or 'TH100103263US'.
     * Some API use this as the ID.
     */
    @JsonProperty
    private String formattedId;
    /**
     * Status of the order.
     */
    @JsonProperty
    private Status status;
    /**
     * Raw shipping status from XiaoXiong.
     */
    @JsonProperty
    private String rawShippingStatus;
    /**
     * When the order is created.
     */
    @JsonProperty
    private DateTime creationTime;
    /**
     * Total price (in $).
     */
    @JsonProperty
    private double price;
    /**
     * Receiver.
     */
    @JsonProperty
    private Receiver receiver;
    /**
     * Product summary.
     */
    @JsonProperty
    private String productSummary;
    /**
     * Products.
     */
    @JsonProperty
    private List<Product> products;
    /**
     * Tracking number for express in China.
     */
    @JsonProperty
    private String trackingNumber;
    /**
     * If Chinese ID card uploaded.
     */
    @JsonProperty
    private Boolean idCardUploaded;
    /**
     * Postman name of express in China.
     */
    @JsonProperty
    private String postmanName;
    /**
     * Postman phone of express in China.
     */
    @JsonProperty
    private String postmanPhone;
    /**
     * Name of the sender.
     */
    @JsonProperty
    private String senderName;
    /**
     * Shipping history.
     */
    @JsonProperty
    private List<ShippingHistoryEntry> shippingHistory;

    @JsonProperty
    private boolean delivered;

    @JsonProperty
    private DeliveryEnding deliveryEnding;

    @JsonProperty
    private Double shippingFee;

    @JsonProperty
    private Double totalWeight;

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

    public long getId() {
        return id;
    }

    public String getFormattedId() {
        return formattedId;
    }

    public Status getStatus() {
        return status;
    }

    public String getRawShippingStatus() {
        return rawShippingStatus;
    }

    public DateTime getCreationTime() {
        return creationTime;
    }

    public double getPrice() {
        return price;
    }

    public Receiver getReceiver() {
        return receiver;
    }

    public String getProductSummary() {
        return productSummary;
    }

    public List<Product> getProducts() {
        return products;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public Boolean getIdCardUploaded() {
        return idCardUploaded;
    }

    public String getPostmanName() {
        return postmanName;
    }

    public String getPostmanPhone() {
        return postmanPhone;
    }

    public String getSenderName() {
        return senderName;
    }

    public List<ShippingHistoryEntry> getShippingHistory() {
        return shippingHistory;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public DeliveryEnding getDeliveryEnding() {
        return deliveryEnding;
    }

    public Double getShippingFee() {
        return shippingFee;
    }

    public Double getTotalWeight() {
        return totalWeight;
    }

    public enum Status {
        CREATED,
        PENDING,
        TRACKING_NUMBER_ASSIGNED,
        POSTMAN_ASSIGNED,
        DELIVERED
    }

    public enum DeliveryEnding {
        PICK_UP_BOX, // 快递柜
        SELF_SIGNED, // 本人签收
        OTHERS_SIGNED, // 他人代签
        UNKNOWN_SIGNED, // Singed by unknown person.
        UNKNOWN;
    }

    public static final class Builder {
        private long id;
        private String formattedId;
        private Status status;
        private String rawShippingStatus;
        private DateTime creationTime;
        private double price;
        private Receiver receiver;
        private String productSummary;
        private List<Product> products = new ArrayList<>();
        private String trackingNumber;
        private Boolean idCardUploaded;
        private String postmanName;
        private String postmanPhone;
        private String senderName;
        private List<ShippingHistoryEntry> shippingHistory = new ArrayList<>();
        private boolean delivered;
        private DeliveryEnding deliveryEnding;
        private Double shippingFee;
        private Double totalWeight;

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

        public Builder withStatus(Status status) {
            this.status = status;
            return this;
        }

        public Builder withRawShippingStatus(String rawShippingStatus) {
            this.rawShippingStatus = rawShippingStatus;
            return this;
        }

        public Builder withCreationTime(DateTime creationTime) {
            this.creationTime = creationTime;
            return this;
        }

        public Builder withPrice(double price) {
            this.price = price;
            return this;
        }

        public Builder withReceiver(Receiver receiver) {
            this.receiver = receiver;
            return this;
        }

        public Builder withProductSummary(String productSummary) {
            this.productSummary = productSummary;
            return this;
        }

        public Builder withProducts(List<Product> products) {
            this.products = products;
            return this;
        }

        public Builder withTrackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
            return this;
        }

        public Builder withIdCardUploaded(Boolean idCardUploaded) {
            this.idCardUploaded = idCardUploaded;
            return this;
        }

        public Builder withPostmanName(String postmanName) {
            this.postmanName = postmanName;
            return this;
        }

        public Builder withPostmanPhone(String postmanPhone) {
            this.postmanPhone = postmanPhone;
            return this;
        }

        public Builder withSenderName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public Builder withShippingHistory(List<ShippingHistoryEntry> shippingHistory) {
            this.shippingHistory = shippingHistory;
            return this;
        }

        public Builder withDelivered(final boolean delivered) {
            this.delivered = delivered;
            return this;
        }

        public Builder withDeliveryEnding(final DeliveryEnding deliveryEnding) {
            this.deliveryEnding = deliveryEnding;
            return this;
        }

        public Builder withShippingFee(final Double shippingFee) {
            this.shippingFee = shippingFee;
            return this;
        }

        public Builder withTotalWeight(final Double totalWeight) {
            this.totalWeight = totalWeight;
            return this;
        }

        public Order build() {
            Order order = new Order();
            order.products = this.products;
            order.shippingHistory = this.shippingHistory;
            order.postmanName = this.postmanName;
            order.productSummary = this.productSummary;
            order.status = this.status;
            order.receiver = this.receiver;
            order.senderName = this.senderName;
            order.formattedId = this.formattedId;
            order.price = this.price;
            order.rawShippingStatus = this.rawShippingStatus;
            order.idCardUploaded = this.idCardUploaded;
            order.id = this.id;
            order.creationTime = this.creationTime;
            order.trackingNumber = this.trackingNumber;
            order.postmanPhone = this.postmanPhone;
            order.delivered = this.delivered;
            order.deliveryEnding = this.deliveryEnding;
            order.shippingFee = this.shippingFee;
            order.totalWeight = this.totalWeight;
            return order;
        }
    }
}
