package jiaoni.common.appengine.access.productsearch;

import com.google.cloud.vision.v1p3beta1.AnnotateImageRequest;
import com.google.cloud.vision.v1p3beta1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1p3beta1.Feature;
import com.google.cloud.vision.v1p3beta1.Image;
import com.google.cloud.vision.v1p3beta1.ImageAnnotatorClient;
import com.google.cloud.vision.v1p3beta1.ImageContext;
import com.google.cloud.vision.v1p3beta1.ImageSource;
import com.google.cloud.vision.v1p3beta1.LocationName;
import com.google.cloud.vision.v1p3beta1.Product;
import com.google.cloud.vision.v1p3beta1.ProductName;
import com.google.cloud.vision.v1p3beta1.ProductSearchClient;
import com.google.cloud.vision.v1p3beta1.ProductSearchParams;
import com.google.cloud.vision.v1p3beta1.ProductSearchResults;
import com.google.cloud.vision.v1p3beta1.ProductSetName;
import com.google.cloud.vision.v1p3beta1.ReferenceImage;
import com.google.common.collect.ImmutableList;
import jiaoni.common.appengine.access.gcp.GoogleClientFactory;
import jiaoni.common.appengine.utils.MediaUtils;
import jiaoni.common.model.Env;
import jiaoni.common.utils.Envs;
import jiaoni.daigou.wiremodel.entity.ProductCategory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GoogleProductSearchClient implements ProSearchClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleProductSearchClient.class);

    private static final String REGION = "us-east1";
    private static final String GOOGLE_PRODUCT_CATEGORY_HOMEGOODS = "homegoods";
    private static final String GOOGLE_PRODUCT_CATEGORY_APPAREL = "apparel";

    private static final String PRODUCT_LABEL_KEY_BRAND = "brand";
    private static final String PRODUCT_LABEL_KEY_CATEGORY = "category";

    private static final LocationName LOCATION_NAME = LocationName.of(Envs.getGaeProjectId(), REGION);

    private final String appName;
    private final Env env;

    public GoogleProductSearchClient(final String appName,
                                     final Env env) {
        this.appName = appName;
        this.env = env;
    }

    @Override
    public jiaoni.daigou.wiremodel.entity.Product addProduct(jiaoni.daigou.wiremodel.entity.Product product) {
        return addProduct(product, ImmutableList.of());
    }

    @Override
    public jiaoni.daigou.wiremodel.entity.Product addProduct(final jiaoni.daigou.wiremodel.entity.Product product, final List<String> mediaIds) {
        try (ProductSearchClient client = GoogleClientFactory.productSearch(GoogleClientFactory.InitStyle.GAE)) {

            final ProductName productName = googleProductName(product.getId());
            final ProductSetName productSetName = googleProductSetName();

            if (StringUtils.isBlank(product.getProductSearchName())) {
                // Create product
                Product googleProduct = Product
                        .newBuilder()
                        .setName(product.getId())
                        .setDisplayName(product.getName())
                        .setProductCategory(toGoogleProductCategory(product.getCategory()))
                        .setDescription(product.getDescription())
                        .addProductLabels(Product.KeyValue.newBuilder().setKey(PRODUCT_LABEL_KEY_BRAND).setValue(product.getBrand()))
                        .addProductLabels(Product.KeyValue.newBuilder().setKey(PRODUCT_LABEL_KEY_CATEGORY).setValue(product.getCategory().name()))
                        .addAllProductLabels(
                                product.getTagsMap()
                                        .entrySet()
                                        .stream()
                                        .map(t -> Product.KeyValue.newBuilder().setKey(t.getKey()).setValue(t.getValue()).build())
                                        .collect(Collectors.toList()))
                        .build();

                LOGGER.info("Create product {}. {}", product.getId(), product);
                client.createProduct(LOCATION_NAME, googleProduct, product.getId());

                // Add it to product set
                LOGGER.info("Add product {} into productSet{}.", productName, productSetName);
                client.addProductToProductSet(productSetName, productName.toString());
            }

            // Add reference images
            for (String mediaId : mediaIds) {
                String gcsPath = MediaUtils.toGcsPath(mediaId);
                ReferenceImage referenceImage = ReferenceImage.newBuilder()
                        .setUri(gcsPath)
                        .build();
                LOGGER.info("create referenceImage for product {}. uri={}", product.getId(), gcsPath);
                client.createReferenceImage(productName, referenceImage, mediaId);
            }

            return product.toBuilder()
                    .setProductSearchName(productName.toString())
                    .build();
        }
    }

    @Override
    public Map<String, Double> search(String mediaId) {
        try (ImageAnnotatorClient imageAnnotatorClient = GoogleClientFactory.betaImageAnnotator()) {
            Image image = Image.newBuilder()
                    .setSource(ImageSource.newBuilder()
                            .setGcsImageUri(MediaUtils.toGcsPath(mediaId)))
                    .build();
            ImageContext imageContext = ImageContext.newBuilder()
                    .setProductSearchParams(ProductSearchParams.newBuilder()
                            .setProductSet(googleProductSetName().toString())
                            .addProductCategories(GOOGLE_PRODUCT_CATEGORY_APPAREL)
                            .addProductCategories(GOOGLE_PRODUCT_CATEGORY_HOMEGOODS))
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(Feature.newBuilder().setType(Feature.Type.PRODUCT_SEARCH))
                    .setImage(image)
                    .setImageContext(imageContext)
                    .build();

            BatchAnnotateImagesResponse response = imageAnnotatorClient.batchAnnotateImages(ImmutableList.of(request));
            List<ProductSearchResults.Result> results = response.getResponses(0).getProductSearchResults().getResultsList();
            Map<String, Double> toReturn = new HashMap<>();
            for (ProductSearchResults.Result result : results) {
                Product product = result.getProduct();
                double conf = result.getScore();
                if (!toReturn.containsKey(product.getName()) || toReturn.get(product.getName()) < conf) {
                    toReturn.put(product.getName(), conf);
                }
            }
            return toReturn;
        }
    }

    private ProductSetName googleProductSetName() {
        return ProductSetName.of(Envs.getGaeProjectId(), REGION, appName + "." + env.name());
    }

    private ProductName googleProductName(final String productId) {
        return ProductName.of(Envs.getGaeProjectId(), REGION, productId);
    }

    private String toGoogleProductCategory(final ProductCategory productCategory) {
        switch (productCategory) {
            case ACCESSORIES:
            case BAGS:
            case WATCHES:
            case CLOTHES:
            case SHOES:
                return GOOGLE_PRODUCT_CATEGORY_APPAREL;
            default:
                return GOOGLE_PRODUCT_CATEGORY_HOMEGOODS;
        }
    }
}
