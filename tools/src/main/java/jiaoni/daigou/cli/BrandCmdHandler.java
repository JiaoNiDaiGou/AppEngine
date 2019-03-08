package jiaoni.daigou.cli;

import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.service.appengine.impls.db.v2.ProductBrandDbClient;
import jiaoni.daigou.v2.entity.ProductBrand;
import sun.tools.jar.CommandLine;

import java.util.List;
import java.util.stream.Collectors;

import static jiaoni.daigou.cli.CliUtils.productBrandDbClient;

class BrandCmdHandler {
    static void handleGetBrands(final CommandLine commandLine,
                                final RemoteApi remoteApi,
                                final Env env) {
        ProductBrandDbClient dbClient = productBrandDbClient(remoteApi, env);
        List<ProductBrand> productBrands = dbClient.scan().collect(Collectors.toList());
    }
}
