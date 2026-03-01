package com.tsengine.common;

import java.time.Instant;

/**
 * Standard immutable API response envelope shared by services.
 *
 * @param success indicates whether the request succeeded
 * @param data payload data for successful responses
 * @param message human-readable status or error message
 * @param timestamp response generation timestamp
 * @param <T> payload type
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        Instant timestamp
) {

    /**
     * Creates a successful API response with current timestamp.
     *
     * @param data response payload
     * @param <T> payload type
     * @return successful API response
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "Success", Instant.now());
    }

    /**
     * Creates an error API response with current timestamp.
     *
     * @param message error message
     * @param <T> payload type
     * @return error API response
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, Instant.now());
    }
}
