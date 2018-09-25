package jiaonidaigou.appengine.api.utils;

import com.google.apphosting.api.ApiProxy;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.common.utils.Environments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppEnvironments extends Environments {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppEnvironments.class);
    public static final Env ENV;

    static {
        ENV = determineEnv();
    }

    private static Env determineEnv() {
        if (ApiProxy.getCurrentEnvironment() == null) {
            return Env.LOCAL;
        }
        ApiProxy.Environment appProxyEnvironment = ApiProxy.getCurrentEnvironment();
        String versionId = appProxyEnvironment.getVersionId();
        String appId = appProxyEnvironment.getAppId();
        LOGGER.info("current App environment. versionId={}, appId={}.", versionId, appId);
        if (appId.contains(GAE_PROJECT_ID)) {
            if (versionId.startsWith("prod.")) {
                return Env.PROD;
            }
            return Env.DEV;
        }
        throw new IllegalStateException("Unknown env " + ApiProxy.getCurrentEnvironment());
    }
}
