package com.tsengine.common;

/**
 * Thrown when configured daily AI cost cap is exceeded.
 */
public class CostCapExceededException extends RuntimeException {

    /**
     * Creates a cost cap exceeded exception with message.
     *
     * @param message exception message
     */
    public CostCapExceededException(String message) {
        super(message);
    }
}
