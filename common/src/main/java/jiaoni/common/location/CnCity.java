package jiaoni.common.location;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

public class CnCity {
    private CnRegion region;
    private String name;
    private List<String> alias;
    private boolean municipality;

    public List<CnZone> getZones() {
        return zones;
    }

    private List<CnZone> zones;

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

    public CnRegion getRegion() {
        return region;
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

    public boolean isMunicipality() {
        return municipality;
    }

    public static final class Builder {
        private CnRegion region;
        private String name;
        private List<String> alias = new ArrayList<>();
        private boolean municipality;
        private List<CnZone> zones = new ArrayList<>();

        private Builder() {
        }

        public Builder withRegion(CnRegion region) {
            this.region = region;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withAlias(List<String> alias) {
            this.alias = alias;
            return this;
        }

        public Builder withMunicipality(boolean municipality) {
            this.municipality = municipality;
            return this;
        }

        public Builder withZones(List<CnZone> zones) {
            this.zones = zones;
            return this;
        }

        public CnCity build() {
            CnCity cnCity = new CnCity();
            cnCity.name = this.name;
            cnCity.municipality = this.municipality;
            cnCity.alias = this.alias;
            cnCity.region = this.region;
            cnCity.zones = this.zones;
            return cnCity;
        }
    }
}
