package jiaoni.common.utils;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Locale;

public class Envs {
    public enum OSType {
        WINDOWS, MAC, LINUX, OTHERS
    }

    /**
     * https://console.cloud.google.com/dataflow?project=fluid-crane-200921
     */
    private static final String GAE_PROJECT_ID = "fluid-crane-200921";

    private static final OSType OS_TYPE;

    private static final String LOCAL_TMP_DIR;
    private static final String GAE_ADMIN_EMAIL = "songfan.rfu@gmail.com";
    private static final List<String> ADMIN_EMAILS = ImmutableList.of("furuijie@gmail.com");

    public static String getGaeProjectId() {
        return GAE_PROJECT_ID;
    }

    public static String getGaeAdminEmail() {
        return GAE_ADMIN_EMAIL;
    }

    public static List<String> getAdminEmails() {
        return ADMIN_EMAILS;
    }

    public static OSType getOSType() {
        return OS_TYPE;
    }

    public static String getLocalTmpDir() {
        return LOCAL_TMP_DIR;
    }

    static {
        OS_TYPE = determineOSType();
        LOCAL_TMP_DIR = determineLocalTempDir();
    }

    public static final String NAMESPACE_SYS = "Sys";

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
