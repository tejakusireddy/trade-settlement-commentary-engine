package com.tsengine.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "services")
public class ServiceUrlsProperties {

    private String tradeIngestUrl;
    private String breachDetectorUrl;
    private String commentaryUrl;

    public String getTradeIngestUrl() {
        return tradeIngestUrl;
    }

    public void setTradeIngestUrl(String tradeIngestUrl) {
        this.tradeIngestUrl = tradeIngestUrl;
    }

    public String getBreachDetectorUrl() {
        return breachDetectorUrl;
    }

    public void setBreachDetectorUrl(String breachDetectorUrl) {
        this.breachDetectorUrl = breachDetectorUrl;
    }

    public String getCommentaryUrl() {
        return commentaryUrl;
    }

    public void setCommentaryUrl(String commentaryUrl) {
        this.commentaryUrl = commentaryUrl;
    }
}
