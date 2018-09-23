package jiaonidaigou.appengine.common.utils;

import com.google.common.collect.ImmutableSet;

import java.util.Locale;
import java.util.Set;

public class Environments {
    public enum OSType {
        WINDOWS, MAC, LINUX, OTHERS
    }

    public static final String SERVICE_NAME_SYS = "Sys";
    public static final String SERVICE_NAME_JIAONIDAIGOU = "JiaoNiDaiGou";
    public static final Set<String> ALL_SERVICE_NAMES = ImmutableSet.of(SERVICE_NAME_JIAONIDAIGOU);
    public static final String GAE_PROJECT_ID = "fluid-crane-200921";
    public static final String GAE_HOSTNAME = GAE_PROJECT_ID + ".appspot.com";
    public static final String MAIN_VERSION_GAE_HOSTNAME = "jiaonidaigou-dot-" + GAE_HOSTNAME;
    public static final String GCS_ROOT_ENDSLASH = "gs://" + GAE_HOSTNAME + "/";
    public static final String GCS_MEDIA_ROOT_ENDSLASH = Environments.GCS_ROOT_ENDSLASH + "media/";
    public static final OSType OS_TYPE;
    public static final String LOCAL_TEMP_DIR_ENDSLASH;
    public static final String LOCAL_ENDPOINT = "http://127.0.0.1:33256";
    public static final String GAE_ADMIN_EMAIL = "songfan.rfu@gmail.com";
    public static final String[] ADMIN_EMAILS = { "furuijie@gmail.com" };

    static {
        OS_TYPE = determineOSType();
        LOCAL_TEMP_DIR_ENDSLASH = determineLocalTempDir();

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
