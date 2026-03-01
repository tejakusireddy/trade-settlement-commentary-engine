package com.tsengine.tradeingest.domain;

import com.tsengine.common.TradeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, UUID> {

    Optional<Trade> findByTradeId(String tradeId);

    List<Trade> findByStatus(TradeStatus status);

    boolean existsByIdempotencyKey(String key);
}
