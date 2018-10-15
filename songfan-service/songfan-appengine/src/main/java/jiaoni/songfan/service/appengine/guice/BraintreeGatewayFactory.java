package jiaoni.songfan.service.appengine.guice;

import com.braintreegateway.BraintreeGateway;
import com.fasterxml.jackson.annotation.JsonProperty;
import jiaoni.common.utils.Secrets;
import jiaoni.songfan.service.appengine.AppEnvs;

import javax.inject.Provider;

public class BraintreeGatewayFactory implements Provider<BraintreeGateway> {
    @Override
    public BraintreeGateway get() {
        Config config = Secrets.of("braintree.config." + AppEnvs.getEnv() + ".json").getAsJson(Config.class);
        return new BraintreeGateway(
                config.environment,
                config.merchatId,
                config.publicKey,
                config.privateKey);
    }

    private static class Config {
        @JsonProperty
        String environment;

        @JsonProperty
        String merchatId;

        @JsonProperty
        String publicKey;

        @JsonProperty
        String privateKey;
    }
}
