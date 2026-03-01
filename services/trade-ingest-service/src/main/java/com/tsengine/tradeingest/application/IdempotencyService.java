package com.tsengine.tradeingest.application;

public interface IdempotencyService {

    Boolean isAlreadyProcessed(String key);

    void markAsProcessed(String key);
}
