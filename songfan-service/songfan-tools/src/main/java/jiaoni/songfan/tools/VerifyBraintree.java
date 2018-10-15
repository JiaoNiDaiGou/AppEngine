package jiaoni.songfan.tools;

import com.braintreegateway.BraintreeGateway;
import jiaoni.common.wiremodel.Price;
import jiaoni.songfan.service.appengine.guice.BraintreeGatewayFactory;
import jiaoni.songfan.service.appengine.impls.BraintreeAccess;

public class VerifyBraintree {
    public static void main(String[] args) {
        BraintreeGateway gateway = new BraintreeGatewayFactory().get();
        BraintreeAccess access = new BraintreeAccess(gateway);

        Price price = Price.newBuilder().setValue(10).build();
        String nonce = "fake-valid-nonce";

        access.creditCardTransaction(price, nonce);
    }
}
