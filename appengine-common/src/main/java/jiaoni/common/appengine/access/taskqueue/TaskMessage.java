package jiaoni.common.appengine.access.taskqueue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.InternalIOException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.function.Consumer;

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
        private int reachCount = 0;
        private String handler;

        private Builder() {
        }

        public Builder withPayload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder withPayloadJson(final Object payloadJson) {
            try {
                return withPayload(ObjectMapperProvider.get().writeValueAsString(payloadJson));
            } catch (JsonProcessingException e) {
                throw new InternalIOException(e);
            }
        }

        public Builder withReachCount(int reachCount) {
            this.reachCount = reachCount;
            return this;
        }

        public Builder increaseReachCount() {
            this.reachCount++;
            return this;
        }

        public Builder withHandler(String handler) {
            this.handler = handler;
            return this;
        }

        public Builder withHandler(final Class<? extends Consumer<TaskMessage>> handleClass) {
            return withHandler(handleClass.getSimpleName());
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
