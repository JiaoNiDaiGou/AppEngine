package jiaoni.wiremodel.common.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jiaoni.daigou.wiremodel.entity.ProductCategory;

import java.util.List;
import java.util.Map;

public class ProductsHints {
    public static class NameAndPrice {
        @JsonProperty("name")
        private String name;

        @JsonProperty("usd")
        private double usd;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getUsd() {
            return usd;
        }

        public void setUsd(double usd) {
            this.usd = usd;
        }

        public static NameAndPrice of(final String name, double usd) {
            NameAndPrice nameAndPrice = new NameAndPrice();
            nameAndPrice.name = name;
            nameAndPrice.usd = usd;
            return nameAndPrice;
        }
    }

    @JsonProperty("hints")
    private Map<ProductCategory, Map<String, List<NameAndPrice>>> hints;

    public Map<ProductCategory, Map<String, List<NameAndPrice>>> getHints() {
        return hints;
    }

    public void setHints(Map<ProductCategory, Map<String, List<NameAndPrice>>> hints) {
        this.hints = hints;
    }
}
