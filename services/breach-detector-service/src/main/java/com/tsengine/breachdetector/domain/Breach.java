package com.tsengine.breachdetector.domain;

import com.tsengine.common.BreachReason;
import com.tsengine.common.BreachType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "settlement_breaches")
public class Breach {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "trade_id", nullable = false)
    private UUID tradeId;

    @Column(nullable = false, length = 100)
    private String instrument;

    @Column(nullable = false, length = 100)
    private String counterparty;

    @Enumerated(EnumType.STRING)
    @Column(name = "breach_type", nullable = false, length = 10)
    private BreachType breachType;

    @Enumerated(EnumType.STRING)
    @Column(name = "breach_reason", nullable = false, length = 50)
    private BreachReason breachReason;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "days_overdue", nullable = false)
    private int daysOverdue;

    @Column(nullable = false, length = 50)
    private String status;

    @PrePersist
    public void prePersist() {
        if (detectedAt == null) {
            detectedAt = Instant.now();
        }
        if (status == null || status.isBlank()) {
            status = "PENDING_COMMENTARY";
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTradeId() {
        return tradeId;
    }

    public void setTradeId(UUID tradeId) {
        this.tradeId = tradeId;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public BreachType getBreachType() {
        return breachType;
    }

    public void setBreachType(BreachType breachType) {
        this.breachType = breachType;
    }

    public BreachReason getBreachReason() {
        return breachReason;
    }

    public void setBreachReason(BreachReason breachReason) {
        this.breachReason = breachReason;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public int getDaysOverdue() {
        return daysOverdue;
    }

    public void setDaysOverdue(int daysOverdue) {
        this.daysOverdue = daysOverdue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
