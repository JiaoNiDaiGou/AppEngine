package jiaoni.daigou.service.appengine.impls.products;

import com.google.common.collect.Table;
import com.google.common.net.MediaType;
import jiaoni.common.appengine.access.storage.StorageClient;
import jiaoni.common.utils.StringUtils2;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.daigou.wiremodel.entity.ProductCategory;
import jiaoni.daigou.wiremodel.entity.ProductHint;
import jiaoni.daigou.wiremodel.entity.ProductHints;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProductHintsFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductHintsFacade.class);

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

    public ProductHints loadHints() {
        ProductHints hints = loadHintsFromGcs();
        if (hints == null || hints.getHintsCount() == 0) {
            LOGGER.info("load no hints from GCS. Rebuild it");
            hints = buildHints();
        }
        return hints;
    }

    public ProductHints buildHints() {
        Set<ProductHint> hints = new HashSet<>();
        addHintsFromDb(hints);
        ProductHints toReturn = ProductHints.newBuilder().addAllHints(hints).build();
        saveHintsIntoGcs(toReturn);
        return toReturn;
    }

    private void addHintsFromDb(final Set<ProductHint> hints) {
        List<Product> products = productFacade.getAll();
        for (Product product : products) {
            if (product.getCategory() == ProductCategory.UNKNOWN
                    || product.getCategory() == ProductCategory.UNRECOGNIZED
                    || StringUtils.isBlank(product.getBrand())
                    || StringUtils.isBlank(product.getName())) {
                continue;
            }
            hints.add(ProductHint.newBuilder()
                    .setCategory(product.getCategory())
                    .setBrand(product.getBrand())
                    .setName(product.getName())
                    .setSuggestedUnitPrice(product.getSuggestedUnitPrice())
                    .build());
        }
    }

    private void saveHintsIntoGcs(final ProductHints hints) {
        byte[] bytes = hints.toByteArray();
        LOGGER.info("Write hints to {}", GCS_PATH);
        storageClient.write(GCS_PATH, MediaType.JSON_UTF_8.toString(), bytes);
    }

    private ProductHints loadHintsFromGcs() {
        byte[] bytes;
        try {
            bytes = storageClient.read(GCS_PATH);
        } catch (Exception e) {
            bytes = null;
        }
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return ProductHints.parseFrom(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
