package jiaonidaigou.appengine.api.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class TaskMessage {
    @JsonProperty
    private String payload;

    @JsonProperty
    private int reachCount;

    @JsonProperty
    private String handler;

    public static Builder builder() {
        return new Builder();
    }

    public String getPayload() {
        return payload;
    }

    public int getReachCount() {
        return reachCount;
    }

    public String getHandler() {
        return handler;
    }

    public Builder toBuilder() {
        return new Builder()
                .withPayload(payload)
                .withReachCount(reachCount)
                .withHandler(handler);
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
        private String payload;
        private int reachCount;
        private String handler;

        private Builder() {
        }

        public Builder withPayload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder withReachCount(int reachCount) {
            this.reachCount = reachCount;
            return this;
        }

        public Builder withHandler(String handler) {
            this.handler = handler;
            return this;
        }

        public TaskMessage build() {
            TaskMessage taskMessage = new TaskMessage();
            taskMessage.reachCount = this.reachCount;
            taskMessage.handler = this.handler;
            taskMessage.payload = this.payload;
            return taskMessage;
        }
    }
}
