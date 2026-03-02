package com.tsengine.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Immutable trade data transfer object shared across services.
 *
 * @param id unique internal trade identifier
 * @param tradeId external business trade identifier
 * @param stableTradeId canonical UUID-form trade identifier used by downstream services
 * @param instrument traded instrument symbol or code
 * @param tradeDate date on which the trade was executed
 * @param expectedSettlementDate expected settlement deadline date
 * @param counterparty counterparty name or identifier
 * @param quantity traded quantity
 * @param price unit price for the trade
 * @param currency currency code used for pricing
 * @param status current trade status
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 */
public record TradeDTO(
        UUID id,
        String tradeId,
        UUID stableTradeId,
        String instrument,
        LocalDate tradeDate,
        LocalDate expectedSettlementDate,
        String counterparty,
        BigDecimal quantity,
        BigDecimal price,
        String currency,
        TradeStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
