package jiaonidaigou.appengine.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

public final class PaginatedResults<T> {
    @JsonProperty("results")
    private final List<T> results;

    @JsonProperty("nextToken")
    private final String nextToken;

    @JsonCreator
    public PaginatedResults(@JsonProperty("results") final List<T> results,
                            @JsonProperty("nextToken") final String nextToken) {
        this.results = results;
        this.nextToken = nextToken;
    }

    public static <T> PaginatedResults<T> empty() {
        return new PaginatedResults<>(ImmutableList.of(), null);
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

    public List<T> getResults() {
        return results;
    }

    public String getNextToken() {
        return nextToken;
    }

}
