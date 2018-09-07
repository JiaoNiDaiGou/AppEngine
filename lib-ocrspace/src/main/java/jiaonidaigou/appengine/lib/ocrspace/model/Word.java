package jiaonidaigou.appengine.lib.ocrspace.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Word {
    @JsonProperty("WordText")
    private String wordText;

    @JsonProperty("Left")
    private long left;

    @JsonProperty("Top")
    private long top;

    @JsonProperty("Height")
    private long height;

    @JsonProperty("Width")
    private long width;

    public String getWordText() {
        return wordText;
    }

    public long getLeft() {
        return left;
    }

    public long getTop() {
        return top;
    }

    public long getHeight() {
        return height;
    }

    public long getWidth() {
        return width;
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
}
