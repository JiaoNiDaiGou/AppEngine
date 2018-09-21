package jiaonidaigou.appengine.tools.model;

import java.util.Arrays;

public class Product {
    private Category category;
    private String name;
    private String brand;
    private int quantity;
    private int unitPriceInDollers;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getUnitPriceInDollers() {
        return unitPriceInDollers;
    }

    public void setUnitPriceInDollers(int unitPriceInDollers) {
        this.unitPriceInDollers = unitPriceInDollers;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
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
}
