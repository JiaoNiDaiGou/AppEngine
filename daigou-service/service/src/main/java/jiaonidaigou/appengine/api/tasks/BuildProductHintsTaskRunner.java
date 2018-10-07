package jiaonidaigou.appengine.api.tasks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.net.MediaType;
import jiaoni.common.appengine.access.storage.StorageClient;
import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.utils.StringUtils2;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.daigou.wiremodel.entity.ProductCategory;
import jiaoni.daigou.wiremodel.entity.ShippingOrder;
import jiaonidaigou.appengine.api.AppEnvs;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BuildProductHintsTaskRunner implements Consumer<TaskMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildProductHintsTaskRunner.class);


    private final StorageClient storageClient;

    @Inject
    public BuildProductHintsTaskRunner(final StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    @Override
    public void accept(TaskMessage taskMessage) {
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

        writeProductsHints(filteredTable);
    }

    private void writeProductsHints(List<Triple<ProductCategory, String, Set<String>>> hints) {
        try {
            byte[] bytes = ObjectMapperProvider.get().writeValueAsBytes(hints);
            String path = AppEnvs.Dir.SHIPPING_ORDERS_ARCHIVE + "latest.json";
            LOGGER.info("Write hints to {}", path);
            storageClient.write(path, MediaType.JSON_UTF_8.toString(), bytes);
        } catch (Exception e) {
            LOGGER.error("Failed to write product hints.", e);
            throw new RuntimeException(e);
        }
    }

    private List<ShippingOrder> loadAllShippingOrders() {
        List<ShippingOrder> toReturn = new ArrayList<>();
        List<String> paths = storageClient.listAll(AppEnvs.Dir.SHIPPING_ORDERS_ARCHIVE);
        for (String path : paths) {
            LOGGER.info("Load {}", path);
            List<ShippingOrder> shippingOrders = new ArrayList<>();
            try {
                shippingOrders = ObjectMapperProvider.get().readValue(storageClient.read(path), new TypeReference<List<ShippingOrder>>() {
                });
            } catch (IOException e) {
                LOGGER.error("Failed read {}. SKIP!", path, e);
            }
            toReturn.addAll(shippingOrders);
        }
        return toReturn;
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
