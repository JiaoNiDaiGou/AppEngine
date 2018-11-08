package jiaoni.daigou.service.appengine.impls.products;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.net.MediaType;
import jiaoni.common.appengine.access.storage.StorageClient;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.utils.StringUtils2;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.daigou.wiremodel.entity.ProductCategory;
import jiaoni.daigou.wiremodel.entity.ShippingOrder;
import jiaoni.wiremodel.common.entity.ProductsHints;
import org.apache.commons.lang3.StringUtils;
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
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProductHintsFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductHintsFacade.class);

    private static final double DEFAULT_SELL_PRICE_USD = 30;

    private static final String GCS_PATH = AppEnvs.Dir.PRODUCTS_HINTS + "latest.json";

    private final ProductFacade productFacade;
    private final StorageClient storageClient;

    @Inject
    public ProductHintsFacade(final ProductFacade productFacade,
                              final StorageClient storageClient) {
        this.productFacade = productFacade;
        this.storageClient = storageClient;
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

    private static boolean contains(final ProductsHints hints, final Product product) {
        if (hints.getHints() == null) {
            hints.setHints(new HashMap<>());
        }
        hints.getHints().computeIfAbsent(product.getCategory(), k -> new HashMap<>());
        hints.getHints().get(product.getCategory()).computeIfAbsent(product.getBrand(), k -> new ArrayList<>());
        return hints.getHints()
                .get(product.getCategory())
                .get(product.getBrand())
                .stream()
                .anyMatch(t -> t.getName().equals(product.getName()));
    }

    private static void add(final ProductsHints hints, final Product product) {
        hints.getHints()
                .get(product.getCategory())
                .get(product.getBrand())
                .add(ProductsHints.NameAndPrice.of(
                        product.getName(),
                        product.hasSuggestedUnitPrice()
                                ? product.getSuggestedUnitPrice().getValue()
                                : DEFAULT_SELL_PRICE_USD));
    }

    private static void add(final ProductsHints hints, final ProductCategory category, final String brand, final Collection<String> names) {
        List<ProductsHints.NameAndPrice> nameAndPrices = names.stream()
                .map(t -> ProductsHints.NameAndPrice.of(t, DEFAULT_SELL_PRICE_USD))
                .collect(Collectors.toList());
        hints.getHints().get(category).get(brand).addAll(nameAndPrices);
    }

    public ProductsHints loadHints() {
        ProductsHints hints = loadHintsFromGcs();
        if (hints == null) {
            hints = buildHints();
        }
        return hints;
    }

    private ProductsHints loadHintsFromGcs() {
        byte[] bytes = storageClient.read(GCS_PATH);
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return ObjectMapperProvider.get().readValue(bytes, ProductsHints.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public ProductsHints buildHints() {
        ProductsHints hints = new ProductsHints();
        addHintsFromDb(hints);
        addHintsFromArchives(hints);
        saveHintsIntoGcs(hints);
        return hints;
    }

    private void addHintsFromDb(final ProductsHints hints) {
        List<Product> products = productFacade.getAll();
        for (Product product : products) {
            if (contains(hints, product)) {
                add(hints, product);
            }
        }
    }

    private void addHintsFromArchives(final ProductsHints hints) {
        List<ShippingOrder> shippingOrders = loadAllShippingOrders();

        List<Product> products = shippingOrders.stream()
                .map(ShippingOrder::getProductEntriesList)
                .flatMap(Collection::stream)
                .map(ShippingOrder.ProductEntry::getProduct)
                .map(t -> t.toBuilder()
                        .setBrand(normBrand(t.getBrand()))
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
            if (category != null) {
                addProduct(table, category, brand, name);
            }
        }

        // If brand is not empty and reference count > 10
        table.cellSet()
                .stream()
                .filter(t -> {
                    String brand = t.getColumnKey();
                    int count = t.getValue().size();
                    return !brand.isEmpty() && count > 10;
                })
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .forEach(t -> add(hints, t.getRowKey(), t.getColumnKey(), t.getValue()));
    }

    private void saveHintsIntoGcs(ProductsHints hints) {
        byte[] bytes;
        try {
            bytes = ObjectMapperProvider.get().writeValueAsBytes(hints);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("Write hints to {}", GCS_PATH);
        storageClient.write(GCS_PATH, MediaType.JSON_UTF_8.toString(), bytes);
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
}
