package com.tsengine.common;

/**
 * Represents settlement breach types based on T+ deadlines.
 */
public enum BreachType {
    T2("Settlement breach at T+2"),
    T3("Settlement breach at T+3"),
    T5("Settlement breach at T+5");

    private final String description;

    BreachType(String description) {
        this.description = description;
    }

    /**
     * Returns the human-readable breach type description.
     *
     * @return breach type description
     */
    public String getDescription() {
        return description;
    }
}
