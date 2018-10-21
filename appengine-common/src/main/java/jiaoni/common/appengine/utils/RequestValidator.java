package jiaoni.common.appengine.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;
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
        validateEmpty(string, null);
    }

    public static void validateEmpty(final String string, final String message) {
        validateRequest(string == null || string.isEmpty(), smartMessage(message, "%s must be empty"));
    }

    public static <T> void validateNotEmpty(final T[] array) {
        validateNotEmpty(array, null);
    }

    public static <T> void validateNotEmpty(final T[] array, final String message) {
        validateRequest(array != null && array.length > 0, smartMessage(message, "%s must not be empty"));
    }

    public static <T> void validateNotEmpty(final Collection<T> collection) {
        validateNotEmpty(collection, null);
    }

    public static <T> void validateNotEmpty(final Collection<T> collection, final String message) {
        validateRequest(collection != null && collection.size() > 0, smartMessage(message, "%s must not be empty"));
    }

    public static <K, V> void validateNotEmpty(final Map<K, V> map, final String message) {
        validateRequest(map != null && map.size() > 0, smartMessage(message, "%s must not be empty"));
    }

    public static <K, V> void validateNotEmpty(final Map<K, V> map) {
        validateNotEmpty(map, null);
    }

    public static void validateNotEmpty(final byte[] array) {
        validateNotEmpty(array, null);
    }

    public static void validateNotEmpty(final byte[] array, final String message) {
        validateRequest(array != null && array.length > 1, "%s must not be empty");
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
}
