package jiaonidaigou.appengine.lib.ocrspace.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

public class TextOverlay {
    @JsonProperty("HasOverlay")
    private boolean hasOverlay;

    @JsonProperty("Message")
    private String message;

    @JsonProperty("Lines")
    private List<Line> lines;

    public boolean isHasOverlay() {
        return hasOverlay;
    }

    public String getMessage() {
        return message;
    }

    public List<Line> getLines() {
        return lines;
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
