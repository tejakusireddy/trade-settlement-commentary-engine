package com.tsengine.common;

/**
 * Thrown when a requested trade cannot be found.
 */
public class TradeNotFoundException extends RuntimeException {

    /**
     * Creates a trade not found exception with message.
     *
     * @param message exception message
     */
    public TradeNotFoundException(String message) {
        super(message);
    }
}
