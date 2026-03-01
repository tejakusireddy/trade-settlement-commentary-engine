package com.tsengine.common;

/**
 * Thrown when commentary generation fails.
 */
public class CommentaryGenerationException extends RuntimeException {

    /**
     * Creates a commentary generation exception with message.
     *
     * @param message exception message
     */
    public CommentaryGenerationException(String message) {
        super(message);
    }
}
