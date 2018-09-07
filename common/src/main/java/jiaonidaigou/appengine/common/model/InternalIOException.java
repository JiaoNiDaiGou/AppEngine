package jiaonidaigou.appengine.common.model;

/**
 * RuntimeException style of {@link java.io.IOException}
 */
public class InternalIOException extends RuntimeException {
    public InternalIOException() {
        super();
    }

    public InternalIOException(String message) {
        super(message);
    }

    public InternalIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalIOException(Throwable cause) {
        super(cause);
    }
}
