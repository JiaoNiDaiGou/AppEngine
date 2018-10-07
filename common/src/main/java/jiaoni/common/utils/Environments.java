package jiaoni.common.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import jiaoni.common.model.Env;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Environments {
    public enum OSType {
        WINDOWS, MAC, LINUX, OTHERS
    }

    public static final String NAMESPACE_JIAONIDAIGOU = "JiaoNiDaiGou";
    public static final String NAMESPACE_SONGFAN = "SongFan";
    public static final String NAMESPACE_WX = "Wx";
    public static final String NAMESPACE_SYS = "Sys";

    public static final Set<String> ALL_OPEN_NAMESPACES = ImmutableSet.of(
            NAMESPACE_JIAONIDAIGOU,
            NAMESPACE_SONGFAN);

    public static final String GAE_PROJECT_ID = "fluid-crane-200921";
    public static final String GAE_HOSTNAME = GAE_PROJECT_ID + ".appspot.com";

    public static final String PROD_VERSION_GAE_HOSTNAME = "prod-dot-" + GAE_HOSTNAME;
    public static final String DEV_VERSION_GAE_HOSTNAME = "dev-dot-" + GAE_HOSTNAME;


    public static final String GCS_ROOT_ENDSLASH = "gs://" + GAE_HOSTNAME + "/";

    public interface Dir {
        String MEDIA_ROOT_ENDSLASH = Environments.GCS_ROOT_ENDSLASH + "media/";
        String SHIPPING_ORDERS_DUMP_ENDSLASH = Environments.GCS_ROOT_ENDSLASH + "teddy_orders_dump/";
        String SHIPPING_ORDERS_ARCHIVE_ENDSLASH = Environments.GCS_ROOT_ENDSLASH + "teddy_orders_archive/";
        String PRODUCTS_HINTS_ENDSLASH = Environments.GCS_ROOT_ENDSLASH + "products_hints/";
    }

    public static final OSType OS_TYPE;
    public static final String LOCAL_TEMP_DIR_ENDSLASH;
    public static final String LOCAL_ENDPOINT = "http://127.0.0.1:33256";
    public static final String GAE_ADMIN_EMAIL = "songfan.rfu@gmail.com";
    public static final String[] ADMIN_EMAILS = { "furuijie@gmail.com" };

    private static final Map<Env, String> HOSTNAMES_BY_ENV = ImmutableMap
            .<Env, String>builder()
            .put(Env.LOCAL, Environments.LOCAL_ENDPOINT)
            .put(Env.DEV, "https://" + Environments.DEV_VERSION_GAE_HOSTNAME)
            .put(Env.PROD, "https://" + Environments.PROD_VERSION_GAE_HOSTNAME)
            .build();

    static {
        OS_TYPE = determineOSType();
        LOCAL_TEMP_DIR_ENDSLASH = determineLocalTempDir();

    }

    public static String getGaeHostNameByEnv(final Env env) {
        return Preconditions.checkNotNull(HOSTNAMES_BY_ENV.get(env));
    }

    private static OSType determineOSType() {
        String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if (os.contains("mac") || os.contains("darwin")) {
            return OSType.MAC;
        } else if (os.contains("win")) {
            return OSType.WINDOWS;
        } else if (os.contains("nux")) {
            return OSType.LINUX;
        } else {
            return OSType.OTHERS;
        }
    }

    private static String determineLocalTempDir() {
        switch (OS_TYPE) {
            case MAC:
                return "/tmp/";
            default:
                return "not supported";
        }
    }
}
