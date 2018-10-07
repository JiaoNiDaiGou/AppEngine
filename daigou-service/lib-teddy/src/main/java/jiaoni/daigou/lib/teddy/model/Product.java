package jiaoni.daigou.lib.teddy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Arrays;

public class Product {
    @JsonProperty
    private Category category;

    @JsonProperty
    private String name;

    @JsonProperty
    private String brand;

    @JsonProperty
    private int quantity;

    @JsonProperty
    private int unitPriceInDollers;

    public static Builder builder() {
        return new Builder();
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getUnitPriceInDollers() {
        return unitPriceInDollers;
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

    public enum Category {
        BAGS("包包"),
        MAKE_UP("化妆品护肤品"),
        WATCH_AND_ACCESSORIES("手表/饰品类"),
        LARGE_ITEMS("大型物品"),
        SMALL_APPLIANCES("小电器类"),
        LARGE_COMMERCIAL_GOODS("大型商货"),
        CLOTHES_AND_SHOES("服装鞋帽"),
        HEALTH_SUPPLEMENTS("保健品"),
        BABY_PRODUCTS("母婴用品"),
        FOOD("食品"),
        TOYS_AND_DAILY_NECESSITIES("玩具/日用品"),
        MILK_POWDER("奶粉"),
        BLANK("");

        private final String val;

        Category(final String val) {
            this.val = val;
        }

        public static Category nameOf(final String name) {
            return Arrays.stream(Category.values())
                    .filter(t -> t.getVal().equals(name))
                    .findFirst()
                    .orElse(BLANK);
        }

        public String getVal() {
            return val;
        }
    }

    public static final class Builder {
        private Category category;
        private String name;
        private String brand;
        private int quantity;
        private int unitPriceInDollers;

        private Builder() {
        }

        public Builder withCategory(Category category) {
            this.category = category;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withBrand(String brand) {
            this.brand = brand;
            return this;
        }

        public Builder withQuantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder withUnitPriceInDollers(int unitPriceInDollers) {
            this.unitPriceInDollers = unitPriceInDollers;
            return this;
        }

        public Product build() {
            Product product = new Product();
            product.quantity = this.quantity;
            product.name = this.name;
            product.category = this.category;
            product.brand = this.brand;
            product.unitPriceInDollers = this.unitPriceInDollers;
            return product;
        }
    }
}
