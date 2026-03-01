package com.tsengine.common;

/**
 * Represents categorized root causes for settlement breaches.
 */
public enum BreachReason {
    MISSING_ASSIGNMENT("Trade assignment details are missing"),
    FAILED_ALLOCATION("Allocation process failed"),
    COUNTERPARTY_FAILURE("Counterparty did not settle"),
    INSUFFICIENT_FUNDS("Settlement account had insufficient funds"),
    SYSTEM_ERROR("Internal system error during settlement");

    private final String description;

    BreachReason(String description) {
        this.description = description;
    }

    /**
     * Returns the human-readable breach reason description.
     *
     * @return breach reason description
     */
    public String getDescription() {
        return description;
    }
}
