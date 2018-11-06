package jiaoni.daigou.service.appengine.impls.products;

import jiaoni.daigou.service.appengine.AppEnvs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

@Singleton
public class ProductsHintsFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductsHintsFacade.class);

    private static final String GCS_PATH = AppEnvs.Dir.PRODUCTS_HINTS + "latest.json";
    private static final String LOCAL_PATH = "products_hints.json";
}
