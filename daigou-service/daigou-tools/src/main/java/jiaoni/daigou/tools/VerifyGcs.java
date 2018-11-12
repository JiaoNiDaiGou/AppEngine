package jiaoni.daigou.tools;

import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.service.appengine.impls.db.ProductDbClient;
import jiaoni.daigou.service.appengine.impls.products.ProductFacade;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.daigou.wiremodel.entity.ProductCategory;

public class VerifyGcs {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            ProductFacade productFacade = new ProductFacade(
                    new ProductDbClient(Env.DEV, remoteApi.getDatastoreService(), remoteApi.getMemcacheService()),
                    null);

            Product product = Product.newBuilder()
                    .setCategory(ProductCategory.ACCESSORIES)
                    .setName("name")
                    .setBrand("brand")
                    .build();

            Product create = productFacade.create(product);
            System.out.println(create.getId());

            Product create2 = productFacade.create(product);
            System.out.println(create2.getId());



//            StorageClient storageClient = new GcsClient(remoteApi.getStorage());
//            List<String> files = storageClient.listAll("gs://fluid-crane-200921.appspot.com/teddy_orders_dump");
//            System.out.println(files);
//
//            ProductHintsFacade productHintsFacade = new ProductHintsFacade(
//                    new ProductFacade(new ProductDbClient(Env.DEV, remoteApi.getDatastoreService(), remoteApi.getMemcacheService()), null),
//                    new GcsClient(remoteApi.getStorage()));
//
//            ProductHints hints = productHintsFacade.loadHints();
//
//            System.out.println(ObjectMapperProvider.compactToJson(hints));

//            StorageClient storageClient = new GcsClient(remoteApi.getStorage());
//            ShippingOrderDbClient shippingOrderDbClient = new ShippingOrderDbClient(Env.PROD, remoteApi.getDatastoreService());
//
//            ShippingOrderFacade facade = new ShippingOrderFacade(storageClient, shippingOrderDbClient);
//
//            List<ShippingOrder> shippingOrders = facade.getAllShippingOrders();
//            List<String> addresses = new ArrayList<>();
//            for (ShippingOrder shippingOrder : shippingOrders) {
//                for (Address address : shippingOrder.getReceiver().getAddressesList()) {
//                    addresses.add(address.getRegion() + "," + address.getCity() + "," + address.getAddress() + "," + address.getPostalCode());
//                }
//            }
//            FileUtils.writeLinesLocalFileAndOpen(addresses);
        }
    }
}
