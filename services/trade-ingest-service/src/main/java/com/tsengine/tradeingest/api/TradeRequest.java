package com.tsengine.tradeingest.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeRequest(
        @NotBlank String tradeId,
        @NotBlank String instrument,
        @NotNull LocalDate tradeDate,
        @NotNull LocalDate expectedSettlementDate,
        @NotBlank String counterparty,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal price,
        @NotBlank String currency,
        @NotBlank String idempotencyKey
) {
}
