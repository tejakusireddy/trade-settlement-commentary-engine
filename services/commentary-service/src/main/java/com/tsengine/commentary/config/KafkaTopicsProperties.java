package com.tsengine.commentary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopicsProperties {

    private String breaches = "trade.breaches";
    private String completed = "commentary.completed";
    private String approved = "commentary.approved";
    private String dlq = "trade.dlq";

    public String getBreaches() {
        return breaches;
    }

    public void setBreaches(String breaches) {
        this.breaches = breaches;
    }

    public String getCompleted() {
        return completed;
    }

    public void setCompleted(String completed) {
        this.completed = completed;
    }

    public String getDlq() {
        return dlq;
    }

    public void setDlq(String dlq) {
        this.dlq = dlq;
    }

    public String getApproved() {
        return approved;
    }

    public void setApproved(String approved) {
        this.approved = approved;
    }
}
