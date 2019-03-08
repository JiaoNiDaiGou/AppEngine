package jiaoni.daigou.lib.wx.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class BaseResponse {
    @JsonProperty("Ret")
    private int ret;

    @JsonProperty("ErrMsg")
    private String errMsg;

    @JsonIgnore
    public boolean hasError() {
        return ret != 0;

        // TODO:
        // Figure out what non-9 ret value means
    }

    public int getRet() {
        return ret;
    }

    public String getErrMsg() {
        return errMsg;
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
