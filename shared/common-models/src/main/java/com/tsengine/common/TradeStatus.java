package com.tsengine.common;

/**
 * Represents the lifecycle status of a trade.
 */
public enum TradeStatus {
    PENDING("Trade is pending settlement"),
    SETTLED("Trade settled successfully"),
    BREACHED("Trade breached settlement deadline"),
    FAILED("Trade processing failed");

    private final String description;

    TradeStatus(String description) {
        this.description = description;
    }

    /**
     * Returns the human-readable status description.
     *
     * @return status description
     */
    public String getDescription() {
        return description;
    }
}
