package com.tsengine.tradeingest.domain;

import com.tsengine.common.TradeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, UUID> {

    Optional<Trade> findByTradeId(String tradeId);

    Optional<Trade> findByIdempotencyKey(String idempotencyKey);

    List<Trade> findByStatus(TradeStatus status);

    Page<Trade> findByStatus(TradeStatus status, Pageable pageable);

    boolean existsByIdempotencyKey(String key);
}
