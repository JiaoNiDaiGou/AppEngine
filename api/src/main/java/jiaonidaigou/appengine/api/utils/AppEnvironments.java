package jiaonidaigou.appengine.api.utils;

import com.google.apphosting.api.ApiProxy;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.common.utils.Environments;

public class AppEnvironments extends Environments {
    public static final Env ENV;

    static {
        ENV = determineEnv();
    }

    private static Env determineEnv() {
        if (ApiProxy.getCurrentEnvironment() == null) {
            return Env.LOCAL;
        } else if (ApiProxy.getCurrentEnvironment().getAppId().contains(Environments.GAE_PROJECT_ID)) { // case your version
            return Env.PROD;
        } else {
            throw new IllegalStateException("Unknown env " + ApiProxy.getCurrentEnvironment());
        }
    }
}
