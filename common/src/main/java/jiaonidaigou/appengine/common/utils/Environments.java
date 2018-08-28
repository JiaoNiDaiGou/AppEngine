package jiaonidaigou.appengine.common.utils;

import java.util.Locale;

public class Environments {
    public enum OSType {
        WINDOWS, MAC, LINUX, OTHERS
    }

    public static final String GAE_PROJECT_ID = "fluid-crane-200921";
    public static final String GAE_HOSTNAME = GAE_PROJECT_ID + ".appspot.com";
    public static final String GCS_ROOT = "gcs://" + GAE_HOSTNAME + "/";
    public static final OSType OS_TYPE;
    public static final String LOCAL_TEMP_DIR;

    static {
        OS_TYPE = determineOSType();
        LOCAL_TEMP_DIR = determineLocalTempDir();
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
