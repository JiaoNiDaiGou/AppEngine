package jiaonidaigou.appengine.api.utils;

import com.google.common.collect.Sets;
import jiaoni.common.utils.Environments;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.function.Supplier;
import javax.ws.rs.BadRequestException;

public class RequestValidator {
    public static void validateRequest(final boolean condition) {
        validateRequest(condition, () -> null);
    }

    public static void validateRequest(final boolean condition, final String message) {
        validateRequest(condition, () -> message);
    }

    public static void validateRequest(final boolean condition, final Supplier<String> message) {
        if (!condition) {
            throw new BadRequestException(message.get());
        }
    }

    public static void validateNotNull(final Object object) {
        validateNotNull(object, null);
    }

    public static void validateNotNull(final Object object, final String message) {
        validateRequest(object != null, smartMessage(message, "%s cannot be null"));
    }

    public static void validateNotBlank(final String string) {
        validateNotBlank(string, null);
    }

    public static void validateNotBlank(final String string, final String message) {
        validateRequest(StringUtils.isNotBlank(string), smartMessage(message, "%s cannot be blank"));
    }

    public static void validateEmpty(final String string) {
        validateNotBlank(string, null);
    }

    public static void validateEmpty(final String string, final String message) {
        validateRequest(string == null || string.isEmpty(), smartMessage(message, "%s must be empty"));
    }

    public static <T> void validateNotEmpty(final T[] array) {
        validateNotEmpty(array, null);
    }

    public static <T> void validateNotEmpty(final T[] array, final String message) {
        validateRequest(array != null && array.length > 1, smartMessage(message, "%s must not be empty"));
    }

    public static void validateNotEmpty(final byte[] array) {
        validateNotEmpty(array, null);
    }

    public static void validateNotEmpty(final byte[] array, final String message) {
        validateRequest(array != null && array.length > 1, "%s must not be empty");
    }

    public static <T> void validateValueInSet(final T toCheck, final Set<T> set, String message) {
        validateRequest(set.contains(toCheck), smartMessage(message, "%s must be in set " + set));
    }

    public static <T> void validateValueInSet(final T toCheck, final Set<T> set) {
        validateValueInSet(toCheck, set, null);
    }

    public static <T> void validateValueInSet(final T toCheck, final T[] set) {
        validateValueInSet(toCheck, Sets.newHashSet(set), null);
    }

    private static Supplier<String> smartMessage(final String message, final String template) {
        return () -> {
            if (StringUtils.isBlank(message)) {
                return String.format(template, "Input");
            }
            if (!message.contains(" ")) {
                // the message is just a name
                return String.format(template, message);
            }
            return message;
        };
    }

    public static void validateAppName(final String appName) {
        RequestValidator.validateValueInSet(appName, Environments.ALL_OPEN_NAMESPACES, appName);
    }
}
