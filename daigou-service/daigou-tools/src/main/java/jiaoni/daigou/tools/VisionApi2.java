package jiaoni.daigou.tools;

import com.google.cloud.vision.v1p3beta1.CreateProductSetRequest;
import com.google.cloud.vision.v1p3beta1.LocationName;
import com.google.cloud.vision.v1p3beta1.Product;
import com.google.cloud.vision.v1p3beta1.ProductName;
import com.google.cloud.vision.v1p3beta1.ProductSearchClient;
import com.google.cloud.vision.v1p3beta1.ProductSet;
import com.google.cloud.vision.v1p3beta1.ProductSetName;
import com.google.cloud.vision.v1p3beta1.ReferenceImage;
import com.google.common.io.ByteStreams;
import jiaoni.common.appengine.access.gcp.GoogleClientFactory;
import jiaoni.common.appengine.access.productsearch.GoogleProductSearchClient;
import jiaoni.common.utils.Envs;
import jiaoni.daigou.wiremodel.entity.ProductCategory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class VisionApi2 {
    private static final String PROJECT_ID = "fluid-crane-200921";
    private static final String REGION = "us-east1";
    private static LocationName LOCATION_NAME = LocationName.of(PROJECT_ID, REGION);

    public static void main(String[] args) throws Exception {
        try (ProductSearchClient client = GoogleClientFactory.productSearch()) {
//            createProductSet(client, "daigou.PROD", "daigou.PROD");
//            createProductSet(client, "daigou.DEV", "daigou.DEV");
//            client.listProductSets(LOCATION_NAME).iterateAll().forEach(t -> System.out.println(t.getName()));
//            client.deleteProductSet("projects/fluid-crane-200921/locations/us-east1/productSets/daigou.DEV");
            Product product = client.getProduct(ProductName.of(PROJECT_ID, REGION, "some_product_id"));
        }
    }

    private static String toProductSetName(final ProductCategory category) {
        return String.format("projects/%s/locations/%s/productSets/%s", Envs.getGaeProjectId(), REGION, category.name());
    }

    public static void createReferenceImage(
            ProductSearchClient client,
            String productId,
            String referenceImageId,
            String localPath)
            throws IOException {
        byte[] bytes;
        try (InputStream inputStream = new FileInputStream(new File(localPath))) {
            bytes = ByteStreams.toByteArray(inputStream);
        }

        // Get the full path of the product.
        ProductName productPath = ProductName.of(PROJECT_ID, REGION, productId);

        // Create a reference image.
        ReferenceImage referenceImage = ReferenceImage.newBuilder()
//                .setUri(gcsUri)
                .build();
        ReferenceImage image =
                client.createReferenceImage(productPath, referenceImage, referenceImageId);

        // Display the reference image information.
        System.out.println(String.format("Reference image name: %s", image.getName()));
        System.out.println(String.format("Reference image uri: %s", image.getUri()));
    }

    public static void addProductToProductSet(ProductSearchClient client, String productId, String productSetId)
            throws IOException {

        // Get the full path of the product set.
        ProductSetName productSetPath = ProductSetName.of(PROJECT_ID, REGION, productSetId);

        // Get the full path of the product.
        String productPath = ProductName.of(PROJECT_ID, REGION, productId).toString();

        // Add the product to the product set.
        client.addProductToProductSet(productSetPath, productPath);

        System.out.println(String.format("Product added to product set."));
    }

    public static void listProductCategories(ProductSearchClient client) {
        client.listProductSets(LOCATION_NAME).iterateAll().forEach(
                t -> {
                    System.out.println(t.getName());
                }
        );
    }

    public static void listProducts(ProductSearchClient client) {
        client.listProducts(LOCATION_NAME)
                .iterateAll()
                .forEach(t -> {
                    System.out.println(t.getName());
                });
    }

    private static void createCategory(ProductSearchClient client) {
    }

    public static void createProductSet(ProductSearchClient client, String productSetId, String productSetDisplayName)
            throws Exception {
        ProductSet myProductSet = ProductSet.newBuilder().setDisplayName(productSetDisplayName).build();
        CreateProductSetRequest request =
                CreateProductSetRequest.newBuilder()
                        .setParent(LOCATION_NAME.toString())
                        .setProductSet(myProductSet)
                        .setProductSetId(productSetId)
                        .build();
        ProductSet productSet = client.createProductSet(request);
//         Display the product set information
        System.out.println(String.format("Product set name: %s", productSet.getName()));

    }

    public static void createProduct(
            ProductSearchClient client,
            String productId,
            String productDisplayName,
            String productCategory)
            throws IOException {

        // Create a product with the product specification in the region.
        // Multiple labels are also supported.
        Product myProduct =
                Product.newBuilder()
                        .setName(productId)
                        .setDisplayName(productDisplayName)
                        .setProductCategory("homegoods")
                        .build();
        Product product = client.createProduct(LOCATION_NAME.toString(), myProduct, productId);

        // Display the product information
        System.out.println(String.format("Product name: %s", product.getName()));
    }
}
