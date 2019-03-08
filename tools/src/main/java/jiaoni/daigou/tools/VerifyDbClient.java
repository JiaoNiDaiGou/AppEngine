package jiaoni.daigou.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.daigou.v2.entity.ProductCategory;

import java.io.FileReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VerifyDbClient {
    private static final String LOCAL_ALL_PRODUCTS = "/tmp/all_products.json";

    public static void main(String[] args) throws Exception {
        List<Prod> products;
        try (FileReader reader = new FileReader(LOCAL_ALL_PRODUCTS)) {
            products = ObjectMapperProvider.get().readValue(reader, new TypeReference<List<Prod>>() {});
        }
        System.out.println(products.size());

        Multimap<ProductCategory, Prod> byCategory = ArrayListMultimap.create();
        for (Prod prod : products) {
            byCategory.put(prod.category, prod);
        }

        for (ProductCategory category : ProductCategory.values()) {
            Collection<Prod> prodsInCategory = byCategory.get(category);
            System.out.println(" ==== " + category + "  ====  has " + prodsInCategory.size() + " products.");
            Set<String> brands = new HashSet<>();
            prodsInCategory.forEach(t -> brands.add(t.brand));
            brands.forEach(t -> System.out.println(t));
            System.out.println("\n");

        }
    }

    private static final class Prod {
        @JsonProperty
        ProductCategory category;
        @JsonProperty
        String name;
        @JsonProperty
        String brand;
    }
}
