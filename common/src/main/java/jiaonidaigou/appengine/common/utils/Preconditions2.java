package jiaonidaigou.appengine.common.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class Preconditions2 {
    public static String checkNotBlank(final String str) {
        checkArgument(isNotBlank(str));
        return str;
    }

    public static String checkNotBlank(final String str, final String message) {
        checkArgument(isNotBlank(str), message);
        return str;
    }
}
