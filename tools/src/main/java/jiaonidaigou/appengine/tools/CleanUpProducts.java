package jiaonidaigou.appengine.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.utils.StringUtils2;
import jiaonidaigou.appengine.wiremodel.entity.Product;
import jiaonidaigou.appengine.wiremodel.entity.ProductCategory;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CleanUpProducts {
    private static List<ShippingOrder> loadAllShippingOrders() throws Exception {

        List<ShippingOrder> toReturn = new ArrayList<>();

        File folder = new File("/Users/ruijie.fu/tmp/great_dump");
        for (File file : folder.listFiles()) {
            System.out.println("handle " + file.getName());
            List<ShippingOrder> shippingOrders = ObjectMapperProvider.get().readValue(file, new TypeReference<List<ShippingOrder>>() {
            });
            toReturn.addAll(shippingOrders);
        }
        return toReturn;
    }

    public static void main(String[] args) throws Exception {
        List<ShippingOrder> shippingOrders = loadAllShippingOrders();
        List<Product> products = shippingOrders.stream()
                .map(ShippingOrder::getProductEntriesList)
                .flatMap(Collection::stream)
                .map(ShippingOrder.ProductEntry::getProduct)
                .map(t -> t.toBuilder().setBrand(normBrand(t.getBrand()))
                        .setName(normName(t.getName()))
                        .build()
                )
                .collect(Collectors.toList());

        List<Product> unknownCategoryProducts = new ArrayList<>();
        Map<String, ProductCategory> brandToCategory = new HashMap<>();
        Table<ProductCategory, String, Set<String>> table = HashBasedTable.create();

        for (Product product : products) {
            ProductCategory category = product.getCategory();
            if (category == ProductCategory.UNKNOWN) {
                unknownCategoryProducts.add(product);
                continue;
            }
            addProduct(table, category, product.getBrand(), product.getName());
            brandToCategory.put(product.getBrand(), category);
        }

        for (Product product : unknownCategoryProducts) {
            String brand = normBrand(product.getBrand());
            String name = product.getName();

            ProductCategory category = brandToCategory.get(brand);
            if (category == null) {
                category = ProductCategory.UNKNOWN;
            }

            addProduct(table, category, brand, name);
        }
        List<Triple<ProductCategory, String, Set<String>>> filteredTable = table.cellSet()
                .stream()
                .filter(t -> {
                    String brand = t.getColumnKey();
                    int count = t.getValue().size();
                    return brand.length() > 1 && count > 10;
                })
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .map(t -> Triple.of(t.getRowKey(), t.getColumnKey(), t.getValue()))
                .collect(Collectors.toList());

        File file = new File("/Users/ruijie.fu/tmp/product_analysis.json");
        ObjectMapperProvider.get().writeValue(file,  filteredTable);
    }

    private static String normBrand(String brand) {
        brand = brand.trim();
        brand = StringUtils.capitalize(brand.toLowerCase());
        return brand;
    }

    private static String normName(String name) {
        name = StringUtils2.removeDuplicatedSpaces(name);
        name = name.replace("*1", "");
        return name;
    }

    private static void addProduct(final Table<ProductCategory, String, Set<String>> table,
                                   final ProductCategory category,
                                   final String brand,
                                   final String name) {
        Set<String> products = table.get(category, brand);
        if (products == null) {
            products = new HashSet<>();
            table.put(category, brand, products);
        }
        products.add(name);
    }
}
