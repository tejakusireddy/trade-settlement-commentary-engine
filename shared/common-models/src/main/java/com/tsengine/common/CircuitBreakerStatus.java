package com.tsengine.common;

/**
 * Represents the current state of the Claude API circuit breaker.
 */
public enum CircuitBreakerStatus {
    CLOSED("Circuit breaker is closed and traffic flows normally"),
    OPEN("Circuit breaker is open and calls are short-circuited"),
    HALF_OPEN("Circuit breaker is half-open and probing recovery");

    private final String description;

    CircuitBreakerStatus(String description) {
        this.description = description;
    }

    /**
     * Returns the human-readable circuit breaker state description.
     *
     * @return circuit breaker state description
     */
    public String getDescription() {
        return description;
    }
}
