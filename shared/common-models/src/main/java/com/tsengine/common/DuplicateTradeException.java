package com.tsengine.common;

/**
 * Thrown when a duplicate trade event is detected.
 */
public class DuplicateTradeException extends RuntimeException {

    /**
     * Creates a duplicate trade exception with message.
     *
     * @param message exception message
     */
    public DuplicateTradeException(String message) {
        super(message);
    }
}
