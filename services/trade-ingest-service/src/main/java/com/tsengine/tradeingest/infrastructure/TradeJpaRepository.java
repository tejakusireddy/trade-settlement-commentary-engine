package com.tsengine.tradeingest.infrastructure;

import com.tsengine.common.TradeStatus;
import com.tsengine.tradeingest.domain.Trade;
import com.tsengine.tradeingest.domain.TradeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class TradeJpaRepository {

    private final TradeRepository tradeRepository;

    public TradeJpaRepository(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    public Trade save(Trade trade) {
        return tradeRepository.save(trade);
    }

    public Optional<Trade> findById(UUID id) {
        return tradeRepository.findById(id);
    }

    public Optional<Trade> findByTradeId(String tradeId) {
        return tradeRepository.findByTradeId(tradeId);
    }

    public List<Trade> findByStatus(TradeStatus status) {
        return tradeRepository.findByStatus(status);
    }

    public boolean existsByIdempotencyKey(String key) {
        return tradeRepository.existsByIdempotencyKey(key);
    }
}
