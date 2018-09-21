package jiaonidaigou.appengine.tools.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static jiaonidaigou.appengine.common.utils.Preconditions2.checkNotBlank;

// TODO:
// Do not use setter
public class Order {
    /**
     * Order ID. It looks like 'RB100093036' or 'TH100103263US'.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Raw ID.
     */
    @JsonProperty("rid")
    private long rId;

    /**
     * Raw order status from XiaoXiong.
     */
    @JsonProperty("rawStatus")
    private String rawStatus;

    /**
     * Raw shipping status from XiaoXiong.
     */
    @JsonProperty("rawShippingStatus")
    private String rawShippingStatus;

    /**
     * When the order is created.
     */
    @JsonProperty("creationTime")
    private DateTime creationTime;

    /**
     * When the order is last updated.
     */
    @JsonProperty("lastUpdateTime")
    private DateTime lastUpdateTime;

    /**
     * Total price (in $).
     */
    @JsonProperty("price")
    private double price;

    /**
     * Receiver.
     */
    @JsonProperty("receiver")
    private Receiver receiver;

    /**
     * Product summary.
     */
    @JsonProperty("productSummary")
    private String productSummary;

    /**
     * Products.
     */
    @JsonProperty("products")
    private List<Product> products;

    /**
     * Tracking number for express in China.
     */
    @JsonProperty("trackingNumber")
    private String trackingNumber;

    /**
     * If Chinese ID card uploaded.
     */
    @JsonProperty("idCardUploaded")
    private Boolean idCardUploaded;

    /**
     * Postman name of express in China.
     */
    @JsonProperty("postmanName")
    private String postmanName;

    /**
     * Postman phone of express in China.
     */
    @JsonProperty("postmanPhone")
    private String postmanPhone;

    @JsonProperty("smsCustomerNotificationSend")
    private boolean smsCustomerNotificationSend;

    @JsonProperty("delivered")
    private boolean delivered;

    @JsonProperty("senderName")
    private String senderName;

    @JsonProperty("senderPhone")
    private String senderPhone;

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public boolean isSmsCustomerNotificationSend() {
        return smsCustomerNotificationSend;
    }

    public void setSmsCustomerNotificationSend(boolean smsCustomerNotificationSend) {
        this.smsCustomerNotificationSend = smsCustomerNotificationSend;
    }

    private List<ShippingHistoryEntry> shippingHistory;

    // OrderId can looks like
    // RB100109371 or TH100103263US
    public Order(final String id, final long rId) {
        this.id = checkNotBlank(id);
        this.rId = rId;
        checkArgument(this.rId != 0);
    }

    // For JSON only.
    private Order() {
    }

    public String getId() {
        return id;
    }

    public long getRId() {
        return rId;
    }

    @JsonIgnore
    public Status getStatus() {
        if (hasTrackingNumber() && hasPostmanPhone()) {
            return Status.POSTMAN_ASSIGNED;
        } else if (hasTrackingNumber()) {
            return Status.TRACKING_NUMBER_ASSIGNED;
        } else if ("运单创建成功".equals(getRawShippingStatus())) {
            return Status.CREATED;
        } else {
            return Status.PENDING;
        }
    }

    public String getRawStatus() {
        return rawStatus;
    }

    public void setRawStatus(String rawStatus) {
        this.rawStatus = rawStatus;
    }

    public String getRawShippingStatus() {
        if (StringUtils.isBlank(rawShippingStatus) && CollectionUtils.isNotEmpty(shippingHistory)) {
            rawShippingStatus = shippingHistory.get(0).getStatus();
        }
        return rawShippingStatus;
    }

    public void setRawShippingStatus(String rawShippingStatus) {
        this.rawShippingStatus = rawShippingStatus;
    }

    public DateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(DateTime creationTime) {
        setLastUpdateTime(creationTime);
        this.creationTime = creationTime;
    }

    public DateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(DateTime lastUpdateTime) {
        if (this.lastUpdateTime == null || this.lastUpdateTime.isBefore(lastUpdateTime)) {
            this.lastUpdateTime = lastUpdateTime;
        }
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Receiver getReceiver() {
        if (receiver == null) {
            receiver = new Receiver();
        }
        return receiver;
    }

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    public String getProductSummary() {
        return productSummary;
    }

    public void setProductSummary(String productSummary) {
        this.productSummary = productSummary;
    }

    public List<Product> getProducts() {
        if (products == null) {
            products = new ArrayList<>();
        }
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public Boolean getIdCardUploaded() {
        return idCardUploaded;
    }

    public void setIdCardUploaded(Boolean idCardUploaded) {
        this.idCardUploaded = idCardUploaded;
    }

    public String getPostmanName() {
        return postmanName;
    }

    public void setPostmanName(String postmanName) {
        this.postmanName = postmanName;
    }

    public String getPostmanPhone() {
        return postmanPhone;
    }

    public boolean hasPostmanPhone() {
        return StringUtils.isNotBlank(postmanPhone);
    }

    public boolean hasTrackingNumber() {
        return StringUtils.isNotBlank(trackingNumber);
    }

    public void setPostmanPhone(String postmanPhone) {
        this.postmanPhone = postmanPhone;
    }

    public List<ShippingHistoryEntry> getShippingHistory() {
        if (shippingHistory == null) {
            shippingHistory = new ArrayList<>();
        }
        return shippingHistory;
    }

    public List<ShippingHistoryEntry> getShippingHistory(final int limit) {
        List<ShippingHistoryEntry> toReturn = new ArrayList<>(getShippingHistory());
        if (toReturn.size() <= limit) {
            return toReturn;
        }
        return toReturn.subList(0, limit);
    }

    public void setShippingHistory(List<ShippingHistoryEntry> shippingHistory) {
        this.shippingHistory = shippingHistory;
        sortShippingHistory();
        if (!this.shippingHistory.isEmpty()) {
            setLastUpdateTime(this.shippingHistory.get(0).timestamp);
        }
    }

    public void addShippingHistory(final DateTime timestamp, final String status) {
        getShippingHistory().add(new ShippingHistoryEntry(timestamp, status));
        sortShippingHistory();
        if (!this.shippingHistory.isEmpty()) {
            setLastUpdateTime(this.shippingHistory.get(0).timestamp);
        }
    }

    private void sortShippingHistory() {
        shippingHistory.sort((a, b) -> b.timestamp.compareTo(a.timestamp));
    }

    public enum Status {
        CREATED,
        PENDING,
        TRACKING_NUMBER_ASSIGNED,
        POSTMAN_ASSIGNED,
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

    /**
     * Shipping history entry.
     */
    public static class ShippingHistoryEntry {
        private DateTime timestamp;
        private String status;

        // For JSON only.
        private ShippingHistoryEntry() {
        }

        public ShippingHistoryEntry(final DateTime timestamp, final String status) {
            this.timestamp = checkNotNull(timestamp);
            this.status = checkNotBlank(status);
        }

        public DateTime getTimestamp() {
            return timestamp;
        }

        public String getStatus() {
            return status;
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
    }

    public static Builder builder(final String id, final long rid) {
        return new Builder(id, rid);
    }

    public static final class Builder {
        private final String id;
        private final long rid;
        private String rawStatus;
        private String rawShippingStatus;
        private DateTime creationTime;
        private DateTime lastUpdateTime;
        private double price;
        private Receiver receiver = new Receiver();
        private String productSummary;
        private List<Product> products;
        private String trackingNumber;
        private Boolean idCardUploaded;
        private String postmanName;
        private String postmanPhone;
        private boolean smsCustomerNotificationSend;
        private boolean delivered;
        private String senderName;
        private String senderPhone;
        private List<ShippingHistoryEntry> shippingHistory = new ArrayList<>();

        private Builder(final String id, final long rid) {
            this.id = id;
            this.rid = rid;
        }

        public Builder withRawStatus(String rawStatus) {
            this.rawStatus = rawStatus;
            return this;
        }

        public Builder withRawShippingStatus(String rawShippingStatus) {
            this.rawShippingStatus = rawShippingStatus;
            return this;
        }

        public Builder withCreationTime(DateTime creationTime) {
            this.creationTime = creationTime;
            if (lastUpdateTime == null) {
                lastUpdateTime = creationTime;
            }
            return this;
        }

        public Builder withCreationTimeNow() {
            return withCreationTime(DateTime.now());
        }

        public Builder withLastUpdateTime(DateTime lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
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

        public Builder withReceiver(String name, String phone) {
            this.receiver.setName(name);
            this.receiver.setPhone(phone);
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

        public Builder withSmsCustomerNotificationSend(boolean smsCustomerNotificationSend) {
            this.smsCustomerNotificationSend = smsCustomerNotificationSend;
            return this;
        }

        public Builder withDelivered(boolean delivered) {
            this.delivered = delivered;
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

        public Builder withShippingHistory(List<ShippingHistoryEntry> shippingHistory) {
            this.shippingHistory = shippingHistory;
            return this;
        }

        public Builder addShippingHistory(final DateTime time, final String status) {
            this.shippingHistory.add(new ShippingHistoryEntry(time, status));
            return this;
        }

        public Order build() {
            Order order = new Order(id, rid);
            order.setRawStatus(rawStatus);
            order.setRawShippingStatus(rawShippingStatus);
            order.setCreationTime(creationTime);
            order.setLastUpdateTime(lastUpdateTime);
            order.setPrice(price);
            order.setReceiver(receiver);
            order.setProductSummary(productSummary);
            order.setProducts(products);
            order.setTrackingNumber(trackingNumber);
            order.setIdCardUploaded(idCardUploaded);
            order.setPostmanName(postmanName);
            order.setPostmanPhone(postmanPhone);
            order.setSmsCustomerNotificationSend(smsCustomerNotificationSend);
            order.setDelivered(delivered);
            order.setSenderName(senderName);
            order.setSenderPhone(senderPhone);
            order.setShippingHistory(shippingHistory);
            return order;
        }
    }
}
