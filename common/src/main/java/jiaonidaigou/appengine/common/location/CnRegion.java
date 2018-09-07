package jiaonidaigou.appengine.common.location;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

public class CnRegion {
    private String name;
    private List<String> alias;

    public static Builder builder() {
        return new Builder();
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

    public String getName() {
        return name;
    }

    public List<String> getAlias() {
        return alias;
    }

    public List<String> getAllPossibleNames() {
        return ImmutableList.<String>builder().add(name).addAll(alias).build();
    }

    public static final class Builder {
        private String name;
        private List<String> alias;

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withAlias(List<String> alias) {
            this.alias = alias;
            return this;
        }

        public CnRegion build() {
            CnRegion cnRegion = new CnRegion();
            cnRegion.alias = this.alias;
            cnRegion.name = this.name;
            return cnRegion;
        }
    }
}
