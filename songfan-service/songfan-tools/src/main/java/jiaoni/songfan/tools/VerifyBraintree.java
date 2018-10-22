package jiaoni.songfan.tools;

import com.braintreegateway.BraintreeGateway;
import jiaoni.common.appengine.access.db.DoNothingMemcache;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.songfan.service.appengine.guice.BraintreeGatewayFactory;
import jiaoni.songfan.service.appengine.impls.BraintreeAccess;
import jiaoni.songfan.service.appengine.impls.CustomerDbClient;
import jiaoni.songfan.service.appengine.impls.MenuDbClient;
import jiaoni.songfan.service.appengine.impls.OrderDbClient;
import jiaoni.songfan.service.appengine.interfaces.publik.MenuInterface;
import jiaoni.songfan.service.appengine.interfaces.publik.OrderInterface;
import jiaoni.songfan.wiremodel.api.InitOrderRequest;
import jiaoni.songfan.wiremodel.entity.Menu;

public class VerifyBraintree {
    public static void main(String[] args) throws Exception {
//        BraintreeGateway gateway = new BraintreeGatewayFactory().get();
//        BraintreeAccess access = new BraintreeAccess(gateway);
//
//        Price price = Price.newBuilder().setValue(10).build();
//        String nonce = "tokencc_bf_v5pvk8_63wx7s_66qdk7_zyysn6_x72";

//        access.creditCardTransaction(price, nonce);

        String json = "{\"customer\":{\"phone\":{\"countryCode\":\"1\",\"phone\":\"4111\"},\"email\":\"\"},\"menuId\":\"test\",\"dishes\":{\"dish_fish\":1},\"paymentType\":\"CreditCard\",\"paymentNonce\":\"tokencc_bf_4r2dky_7dtyyx_9q54qg_nwhmdh_7by\"}";
        InitOrderRequest fromJson = ObjectMapperProvider.get().readValue(json, InitOrderRequest.class);
//        System.out.println(fromJson);
//        System.out.println(fromJson.getDishesMap());
//        RequestValidator.validateNotEmpty(fromJson.getDishesMap());

        try (RemoteApi remoteApi = RemoteApi.login()) {
            MenuDbClient menuDbClient = new MenuDbClient(Env.DEV, remoteApi.getDatastoreService(), new DoNothingMemcache());
            OrderInterface orderInterface = new OrderInterface(
                    new CustomerDbClient(Env.DEV, remoteApi.getDatastoreService()),
                    menuDbClient,
                    new OrderDbClient(Env.DEV, remoteApi.getDatastoreService()),
                    new BraintreeAccess(new BraintreeGatewayFactory().get())
            );
//            orderInterface.init(fromJson);

            Menu menu = menuDbClient.getById("test");
            System.out.println(menu);
        }
    }
}
