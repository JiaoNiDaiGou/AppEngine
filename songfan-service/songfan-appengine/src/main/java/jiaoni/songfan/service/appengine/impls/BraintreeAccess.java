package jiaoni.songfan.service.appengine.impls;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;
import com.google.common.base.Preconditions;
import jiaoni.common.utils.Preconditions2;
import jiaoni.common.wiremodel.Price;

import java.math.BigDecimal;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BraintreeAccess {
    private final BraintreeGateway gateway;

    @Inject
    public BraintreeAccess(final BraintreeGateway gateway) {
        this.gateway = gateway;
    }

    public void creditCardTransaction(final Price price,
                                      final String paymentMethodNonce) {
        Preconditions.checkNotNull(price);
        Preconditions2.checkNotBlank(paymentMethodNonce);

        TransactionRequest request = new TransactionRequest()
                .amount(new BigDecimal(price.getValue()))
                .paymentMethodNonce(paymentMethodNonce)
                .options()
                .submitForSettlement(true)
                .done();

        Result<Transaction> result = gateway.transaction().sale(request);
        if (result.isSuccess()) {
            System.out.println(result.getTarget());
            // See result.getTarget() for details
        } else {
            System.out.println(result.getTarget());
            // Handle errors
        }
    }
}
