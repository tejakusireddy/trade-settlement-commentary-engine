package com.tsengine.commentary.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "anthropic")
@Validated
public class AnthropicProperties {

    @NotBlank
    private String apiKey;

    @NotBlank
    private String model = "claude-sonnet-4-6";

    @NotNull
    private BigDecimal dailyCostCapUsd = new BigDecimal("10.00");

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public BigDecimal getDailyCostCapUsd() {
        return dailyCostCapUsd;
    }

    public void setDailyCostCapUsd(BigDecimal dailyCostCapUsd) {
        this.dailyCostCapUsd = dailyCostCapUsd;
    }
}
