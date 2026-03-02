package com.tsengine.commentary.application;

import com.tsengine.commentary.config.AnthropicProperties;
import com.tsengine.commentary.infrastructure.RedisCostStore;
import com.tsengine.common.CostCapExceededException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class CostTrackingService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Duration COST_KEY_TTL = Duration.ofHours(25);

    private final RedisCostStore redisCostStore;
    private final AnthropicProperties anthropicProperties;

    public CostTrackingService(RedisCostStore redisCostStore, AnthropicProperties anthropicProperties) {
        this.redisCostStore = redisCostStore;
        this.anthropicProperties = anthropicProperties;
    }

    public void incrementDailyCost(BigDecimal amount) {
        String key = dailyCostKey();
        redisCostStore.incrementByFloat(key, amount);
        redisCostStore.expireIfNoTtl(key, COST_KEY_TTL);
    }

    public BigDecimal getDailyCost() {
        return redisCostStore.get(dailyCostKey());
    }

    public boolean isCostCapExceeded() {
        return getDailyCost().compareTo(getDailyCostCap()) >= 0;
    }

    public BigDecimal getDailyCostCap() {
        return anthropicProperties.getDailyCostCapUsd();
    }

    public void validateUnderCap() {
        if (isCostCapExceeded()) {
            throw new CostCapExceededException("Daily Claude cost cap exceeded");
        }
    }

    public String dailyCostKey() {
        String date = LocalDate.now(ZoneOffset.UTC).format(DATE_FORMATTER);
        return "ai:cost:daily:" + date;
    }
}
