package jiaonidaigou.appengine.common.model;

/**
 * Thrown when service internal failed.
 */
public class InternalRuntimeException extends RuntimeException {
    public InternalRuntimeException() {
        super();
    }

    public InternalRuntimeException(String message) {
        super(message);
    }

    public InternalRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalRuntimeException(Throwable cause) {
        super(cause);
    }
}
