package jiaoni.daigou.tools;

import com.google.common.base.Stopwatch;
import jiaoni.common.appengine.access.ocr.GoogleVisionOcrClient;
import jiaoni.common.appengine.access.ocr.OcrClient;
import jiaoni.common.httpclient.BrowserClient;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClientImpl;
import jiaoni.daigou.lib.teddy.model.Order;
import jiaoni.daigou.lib.teddy.model.Product;
import jiaoni.daigou.lib.teddy.model.Receiver;
import jiaoni.daigou.service.appengine.impls.teddy.GOcrTeddyLoginGuidRecognizer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class VerifyTeddyClient {
    public static void main(String[] args) throws Exception {
        makeOrder();
    }

    private static void makeOrder() throws Exception {
        OcrClient ocrClient = new GoogleVisionOcrClient();
        GOcrTeddyLoginGuidRecognizer loginGuidRecognizer = new GOcrTeddyLoginGuidRecognizer(ocrClient);
        TeddyClientImpl client = new TeddyClientImpl(TeddyAdmins.HACK, new BrowserClient(), loginGuidRecognizer);

//        List<Product.Category> categories = client.getCategories();
//        System.out.println(categories);

        Receiver receiver = Receiver.builder()
                .withName("王大毛")
                .withPhone("12345678901")
                .withAddress("东方路3号")
                .withAddressZone("买市区")
                .withAddressCity("买买市")
                .withAddressRegion("上海")
                .build();
        Product product = Product.builder()
                .withBrand("大华")
                .withName("毛巾")
                .withQuantity(1)
                .withCategory(Product.Category.CLOTHES_AND_SHOES)
                .withUnitPriceInDollers(10)
                .build();
        Stopwatch stopwatch = Stopwatch.createStarted();
        Order order = client.makeOrder(receiver, Arrays.asList(product), 0.2);
        long taken = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        System.out.println(order.getFormattedId() + " in " + taken);
    }
}
