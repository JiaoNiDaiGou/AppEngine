package jiaoni.songfan.service.appengine;

import com.google.apphosting.api.ApiProxy;
import com.google.common.collect.ImmutableMap;
import jiaoni.common.model.Env;
import jiaoni.common.utils.Envs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class AppEnvs extends Envs {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppEnvs.class);

    private static final Env ENV;
    private static final Map<Env, String> HOSTNAMES_BY_ENV;
    private static final String GCS_ROOT = "gs://songfan/";

    static {
        ENV = determineEnv();
        HOSTNAMES_BY_ENV = ImmutableMap.of(
                Env.LOCAL, "http://127.0.0.1:44256",
                Env.DEV, "https://dev-dot-songfan-dot-" + Envs.getGaeProjectId() + ".appspot.com",
                Env.PROD, "https://prod-dot-songfan-dot-" + Envs.getGaeProjectId() + ".appspot.com"
        );
    }

    private AppEnvs() {
    }

    public static String getHostname(final Env env) {
        checkNotNull(env);
        return HOSTNAMES_BY_ENV.get(env);
    }

    public static String getServiceName() {
        return "SongFan";
    }

    public static Env getEnv() {
        return ENV;
    }

    private static Env determineEnv() {
        if (ApiProxy.getCurrentEnvironment() == null) {
            return Env.LOCAL;
        }
        ApiProxy.Environment appProxyEnvironment = ApiProxy.getCurrentEnvironment();
        String versionId = appProxyEnvironment.getVersionId();
        String appId = appProxyEnvironment.getAppId();
        LOGGER.info("current App environment. versionId={}, appId={}.", versionId, appId);
        if (appId.contains(Envs.getGaeProjectId())) {
            if (versionId.startsWith("prod")) {
                return Env.PROD;
            }
            return Env.DEV;
        }
        throw new IllegalStateException("Unknown env " + ApiProxy.getCurrentEnvironment());
    }

    public interface Dir {
        String MEDIA_ROOT = GCS_ROOT + "media/";
    }
}
