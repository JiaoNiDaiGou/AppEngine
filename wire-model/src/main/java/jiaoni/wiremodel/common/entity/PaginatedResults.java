package jiaoni.wiremodel.common.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.List;

public final class PaginatedResults<T> {
    @JsonProperty
    private List<T> results;

    @JsonProperty
    private String pageToken;

    @JsonProperty
    private Integer totoalCount;

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static <T> PaginatedResults<T> empty() {
        return new Builder<T>().build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaginatedResults<?> that = (PaginatedResults<?>) o;
        return Objects.equal(results, that.results) &&
                Objects.equal(pageToken, that.pageToken);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(results, pageToken);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("results", results)
                .add("pageToken", pageToken)
                .toString();
    }

    public List<T> getResults() {
        return results;
    }

    public String getPageToken() {
        return pageToken;
    }

    public Integer getTotoalCount() {
        return totoalCount;
    }

    public static final class Builder<T> {
        private List<T> results = new ArrayList<>();
        private String pageToken;
        private Integer totoalCount;

        private Builder() {
        }

        public Builder<T> withResults(List<T> results) {
            this.results = results;
            return this;
        }

        public Builder<T> withPageToken(String pageToken) {
            this.pageToken = pageToken;
            return this;
        }

        public Builder<T> withTotoalCount(Integer totoalCount) {
            this.totoalCount = totoalCount;
            return this;
        }

        public PaginatedResults<T> build() {
            PaginatedResults<T> paginatedResults = new PaginatedResults<>();
            paginatedResults.totoalCount = this.totoalCount;
            paginatedResults.results = this.results;
            paginatedResults.pageToken = this.pageToken;
            return paginatedResults;
        }
    }
}
