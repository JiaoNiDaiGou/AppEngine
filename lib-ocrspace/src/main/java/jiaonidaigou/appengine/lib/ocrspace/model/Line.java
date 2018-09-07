package jiaonidaigou.appengine.lib.ocrspace.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

public class Line {
    @JsonProperty("Words")
    private List<Word> words;

    @JsonProperty("MaxHeight")
    private long maxHeight;

    @JsonProperty("MinTop")
    private long minTop;

    public List<Word> getWords() {
        return words;
    }

    public long getMaxHeight() {
        return maxHeight;
    }

    public long getMinTop() {
        return minTop;
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
