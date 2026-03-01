package com.tsengine.commentary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "ai_usage_audit")
public class AiUsageAudit {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "commentary_id", nullable = false)
    private UUID commentaryId;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "tokens_input", nullable = false)
    private int tokensInput;

    @Column(name = "tokens_output", nullable = false)
    private int tokensOutput;

    @Column(name = "cost_usd", nullable = false, precision = 10, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "prompt_version", nullable = false, length = 20)
    private String promptVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCommentaryId() {
        return commentaryId;
    }

    public void setCommentaryId(UUID commentaryId) {
        this.commentaryId = commentaryId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTokensInput() {
        return tokensInput;
    }

    public void setTokensInput(int tokensInput) {
        this.tokensInput = tokensInput;
    }

    public int getTokensOutput() {
        return tokensOutput;
    }

    public void setTokensOutput(int tokensOutput) {
        this.tokensOutput = tokensOutput;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public void setCostUsd(BigDecimal costUsd) {
        this.costUsd = costUsd;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
