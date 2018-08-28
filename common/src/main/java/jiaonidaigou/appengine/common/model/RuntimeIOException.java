package jiaonidaigou.appengine.common.model;

/**
 * RuntimeException style of {@link java.io.IOException}
 */
public class RuntimeIOException extends RuntimeException {
    public RuntimeIOException() {
        super();
    }

    public RuntimeIOException(String message) {
        super(message);
    }

    public RuntimeIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeIOException(Throwable cause) {
        super(cause);
    }
}
