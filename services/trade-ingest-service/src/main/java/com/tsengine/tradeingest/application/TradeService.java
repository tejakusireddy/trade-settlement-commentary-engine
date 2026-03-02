package com.tsengine.tradeingest.application;

import com.tsengine.common.DuplicateTradeException;
import com.tsengine.common.TradeDTO;
import com.tsengine.common.TradeNotFoundException;
import com.tsengine.tradeingest.api.TradeMapper;
import com.tsengine.tradeingest.api.TradeRequest;
import com.tsengine.tradeingest.domain.Trade;
import com.tsengine.tradeingest.domain.TradeRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tsengine.common.TradeStatus;

@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final IdempotencyService idempotencyService;
    private final TradeEventPublisher tradeEventPublisher;
    private final TradeMapper tradeMapper;

    @Qualifier("tradesIngestedSuccessCounter")
    private final Counter successCounter;
    @Qualifier("tradesIngestedDuplicateCounter")
    private final Counter duplicateCounter;
    @Qualifier("tradesIngestedFailedCounter")
    private final Counter failedCounter;
    @Qualifier("tradeIngestLatencyTimer")
    private final Timer latencyTimer;

    public TradeService(
            TradeRepository tradeRepository,
            IdempotencyService idempotencyService,
            TradeEventPublisher tradeEventPublisher,
            TradeMapper tradeMapper,
            @Qualifier("tradesIngestedSuccessCounter") Counter successCounter,
            @Qualifier("tradesIngestedDuplicateCounter") Counter duplicateCounter,
            @Qualifier("tradesIngestedFailedCounter") Counter failedCounter,
            Timer latencyTimer
    ) {
        this.tradeRepository = tradeRepository;
        this.idempotencyService = idempotencyService;
        this.tradeEventPublisher = tradeEventPublisher;
        this.tradeMapper = tradeMapper;
        this.successCounter = successCounter;
        this.duplicateCounter = duplicateCounter;
        this.failedCounter = failedCounter;
        this.latencyTimer = latencyTimer;
    }

    @Transactional
    public TradeDTO ingestTrade(TradeRequest request) {
        Timer.Sample sample = Timer.start();
        try {
            if (Boolean.TRUE.equals(idempotencyService.isAlreadyProcessed(request.idempotencyKey()))
                    || tradeRepository.existsByIdempotencyKey(request.idempotencyKey())) {
                duplicateCounter.increment();
                Trade duplicate = resolveDuplicateTrade(request)
                        .orElseThrow(() -> new DuplicateTradeException(
                                "Trade already processed for idempotencyKey=" + request.idempotencyKey()));
                return tradeMapper.toDto(duplicate);
            }

            Trade trade = tradeMapper.toEntity(request);
            Trade savedTrade = tradeRepository.save(trade);
            tradeEventPublisher.publishTradeEvent(savedTrade);
            idempotencyService.markAsProcessed(request.idempotencyKey());

            successCounter.increment();
            return tradeMapper.toDto(savedTrade);
        } catch (DuplicateTradeException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            failedCounter.increment();
            throw ex;
        } finally {
            sample.stop(latencyTimer);
        }
    }

    @Transactional
    public List<TradeDTO> ingestBatch(List<TradeRequest> requests) {
        List<TradeDTO> results = new ArrayList<>();
        for (TradeRequest request : requests) {
            try {
                results.add(ingestTrade(request));
            } catch (DuplicateTradeException ex) {
                // Best-effort batch ingest: continue with the rest.
            }
        }
        return results;
    }

    @Transactional(readOnly = true)
    public TradeDTO getTrade(UUID id) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("Trade not found for id=" + id));
        return tradeMapper.toDto(trade);
    }

    @Transactional(readOnly = true)
    public Page<TradeDTO> listTrades(int page, int size, TradeStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Trade> trades = status == null
                ? tradeRepository.findAll(pageable)
                : tradeRepository.findByStatus(status, pageable);
        return trades.map(tradeMapper::toDto);
    }

    private Optional<Trade> resolveDuplicateTrade(TradeRequest request) {
        return tradeRepository.findByIdempotencyKey(request.idempotencyKey())
                .or(() -> tradeRepository.findByTradeId(request.tradeId()));
    }
}
