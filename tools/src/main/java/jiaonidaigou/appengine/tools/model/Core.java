package jiaonidaigou.appengine.tools.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Core {
    public Core(UserMode userMode) {
        this.userMode = checkNotNull(userMode);
    }

    // For JSON only
    private Core() {
    }

    @JsonProperty
    private String viewState;

    @JsonProperty
    private boolean loggedIn;

    @JsonProperty
    private Map<String, Receiver> receivers = new HashMap<>();

    @JsonProperty
    private SetMultimap<Order.Status, Long> trackingOrderRIds = HashMultimap.create();

    @JsonProperty
    private Set<Long> notNotifyCustomer = new HashSet<>();

    @JsonProperty
    private UserMode userMode;

    public UserMode getUserMode() {
        return userMode;
    }

    public void setUserMode(UserMode userMode) {
        this.userMode = userMode;
    }

    public String getViewState() {
        return viewState;
    }

    public void setViewState(String viewState) {
        this.viewState = viewState;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    /**
     * Add receiver. Return true if the map already contains it.
     */
    public boolean addReceiver(final Receiver receiver) {
        return receivers.put(receiver.getPhone(), receiver) != null;
    }

    public void addReceivers(final List<Receiver> receivers) {
        receivers.forEach(this::addReceiver);
    }

    public Map<String, Receiver> getReceivers() {
        return receivers;
    }

    public List<Receiver> getAllReceivers() {
        return new ArrayList<>(receivers.values());
    }

    public void setReceivers(Map<String, Receiver> receivers) {
        this.receivers = receivers;
    }

    public SetMultimap<Order.Status, Long> getTrackingOrderRIds() {
        return trackingOrderRIds;
    }

    public void setTrackingOrderRIds(SetMultimap<Order.Status, Long> trackingOrderRIds) {
        this.trackingOrderRIds = trackingOrderRIds;
    }

    public void clearTrackingOrderRIds() {
        this.trackingOrderRIds.clear();
    }

    public void addTrackingOrderRId(Order.Status status, final long orderRId) {
        checkNotNull(status);
        checkArgument(orderRId != 0);
        this.trackingOrderRIds.put(status, orderRId);
    }

    public void addTrackingOrderRId(final Order order) {
        checkNotNull(order);
        this.trackingOrderRIds.put(order.getStatus(), order.getRId());
    }

    public void addTrackingOrderRIds(final Order... orders) {
        for (Order order : orders) {
            addTrackingOrderRId(order);
        }
    }

    public Set<Long> getNotNotifyCustomer() {
        return notNotifyCustomer;
    }

    public void setNotNotifyCustomer(Set<Long> notNotifyCustomer) {
        this.notNotifyCustomer = notNotifyCustomer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Core core = (Core) o;
        return loggedIn == core.loggedIn &&
                userMode == core.userMode &&
                Objects.equal(viewState, core.viewState) &&
                Objects.equal(receivers, core.receivers) &&
                Objects.equal(trackingOrderRIds, core.trackingOrderRIds);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(viewState, loggedIn, receivers, trackingOrderRIds, userMode);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("viewState", viewState)
                .add("loggedIn", loggedIn)
                .add("receivers", receivers)
                .add("trackingOrderRIds", trackingOrderRIds)
                .add("userMode", userMode)
                .toString();
    }
}
