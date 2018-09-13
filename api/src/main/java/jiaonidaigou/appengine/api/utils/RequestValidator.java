package jiaonidaigou.appengine.api.utils;

import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.BadRequestException;

public class RequestValidator {
    public static void validateRequest(final boolean condition) {
        validateRequest(condition, null);
    }

    public static void validateRequest(final boolean condition, final String message) {
        if (!condition) {
            throw new BadRequestException(message);
        }
    }

    public static void validateNotNull(final Object object) {
        validateNotNull(object, null);
    }

    public static void validateNotNull(final Object object, final String message) {
        String fullMessage = message;
        if (StringUtils.isNotBlank(message) && !message.contains(" ")) {
            // Single word
            fullMessage = message + " cannot be blank";
        }
        validateRequest(object != null, fullMessage);
    }

    public static void validateNotBlank(final String string) {
        validateNotBlank(string, null);
    }

    public static void validateNotBlank(final String string, final String message) {
        String fullMessage = message;
        if (StringUtils.isNotBlank(message) && !message.contains(" ")) {
            // Single word
            fullMessage = message + " cannot be blank";
        }
        validateRequest(StringUtils.isNotBlank(string), fullMessage);
    }

    public static void validateEmpty(final String string) {
        validateNotBlank(string, null);
    }

    public static void validateEmpty(final String string, final String message) {
        String fullMessage = message;
        if (StringUtils.isNotBlank(message) && !message.contains(" ")) {
            // Single word
            fullMessage = message + " must be empty";
        }
        validateRequest(string == null || string.isEmpty(), fullMessage);
    }

    public static <T> void validateNotEmpty(final T[] array) {
        validateNotEmpty(array, null);
    }

    public static <T> void validateNotEmpty(final T[] array, final String message) {
        String fullMessage = message;
        if (StringUtils.isNotBlank(message) && !message.contains(" ")) {
            // Single word
            fullMessage = message + " must not be empty";
        }
        validateRequest(array != null && array.length > 1, fullMessage);
    }

    public static void validateNotEmpty(final byte[] array) {
        validateNotEmpty(array, null);
    }

    public static void validateNotEmpty(final byte[] array, final String message) {
        String fullMessage = message;
        if (StringUtils.isNotBlank(message) && !message.contains(" ")) {
            // Single word
            fullMessage = message + " must not be empty";
        }
        validateRequest(array != null && array.length > 1, fullMessage);
    }
}
