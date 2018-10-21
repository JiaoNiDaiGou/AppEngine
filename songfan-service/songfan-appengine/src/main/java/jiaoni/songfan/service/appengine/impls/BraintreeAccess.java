package jiaoni.songfan.service.appengine.impls;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.ClientTokenRequest;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;
import jiaoni.common.wiremodel.Price;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;
import static jiaoni.common.utils.Preconditions2.checkNotBlank;

@Singleton
public class BraintreeAccess {
    private static final Logger LOGGER = LoggerFactory.getLogger(BraintreeAccess.class);
    private final BraintreeGateway gateway;

    @Inject
    public BraintreeAccess(final BraintreeGateway gateway) {
        this.gateway = gateway;
    }

    public static PaymentMethod paymentMethodOf(final String val) {
        if ("CreditCard".equalsIgnoreCase(val)) {
            return PaymentMethod.CREDIT_CARD;
        }
        throw new IllegalArgumentException("Unknown payment method type: " + val);
    }

    public TransactionResult creditCardTransaction(final Price price,
                                                   final String paymentMethodNonce) {
        checkNotNull(price);
        checkNotBlank(paymentMethodNonce);

        TransactionRequest request = new TransactionRequest()
                .amount(new BigDecimal(price.getValue()))
                .paymentMethodNonce(paymentMethodNonce)
                .options()
                .submitForSettlement(true)
                .done();

        Result<Transaction> result = gateway.transaction().sale(request);
        if (result.isSuccess()) {
            LOGGER.info("Success make transaction!! price:${}. transaction.targetId {}.",
                    price.getValue(),
                    result.getTarget().getId());
            return TransactionResult.success(result.getTarget().getId());
        } else {
            LOGGER.error("Failed to make transaction!! price:${}. errorMessage: {}", result.getMessage());
            return TransactionResult.error(400, result.getMessage());
        }
    }

    public String getClientToken(@Nullable final String customerId) {
        if (StringUtils.isBlank(customerId)) {
            return gateway.clientToken().generate();
        } else {
            ClientTokenRequest request = new ClientTokenRequest()
                    .customerId(customerId);
            return gateway.clientToken().generate(request);
        }
    }

    public enum PaymentMethod {
        CREDIT_CARD
    }

    public static class TransactionResult {
        private final int errorCode;
        private final String errorMessage;
        private final String transactionId;

        TransactionResult(int errorCode, String errorMessage, String transactionId) {
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
            this.transactionId = transactionId;
        }

        static TransactionResult error(int code, String message) {
            return new TransactionResult(code, message, null);
        }

        static TransactionResult success(String transactionId) {
            return new TransactionResult(0, null, transactionId);
        }

        public int getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isSuccess() {
            return errorCode == 0;
        }

        public String getTransactionId() {
            return transactionId;
        }
    }
}
