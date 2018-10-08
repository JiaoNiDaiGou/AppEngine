package jiaoni.songfan.service.integrationtest;

import jiaoni.common.model.Env;
import jiaoni.common.test.ApiClient;
import jiaoni.songfan.service.appengine.AppEnvs;
import org.junit.Test;

public class OrderIntegrationTest {
    private final ApiClient apiClient = new ApiClient(AppEnvs.getHostname(Env.DEV));

    @Test
    public void testInitOrder_knownCustomer() {

    }

    @Test
    public void testInitOrder_uknownCustomer() {

    }
}
