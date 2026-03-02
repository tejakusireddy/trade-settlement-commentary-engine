package com.tsengine.tradeingest.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tsengine.common.TradeDTO;
import com.tsengine.common.TradeNotFoundException;
import com.tsengine.common.TradeStatus;
import com.tsengine.tradeingest.api.TradeMapper;
import com.tsengine.tradeingest.application.TradeEventPublisher;
import com.tsengine.tradeingest.api.TradeRequest;
import com.tsengine.tradeingest.application.IdempotencyService;
import com.tsengine.tradeingest.application.TradeService;
import com.tsengine.tradeingest.domain.Trade;
import com.tsengine.tradeingest.domain.TradeRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private TradeEventPublisher tradeEventPublisher;
    @Mock
    private Counter successCounter;
    @Mock
    private Counter duplicateCounter;
    @Mock
    private Counter failedCounter;
    @Mock
    private Timer latencyTimer;

    private TradeService tradeService;
    private TradeMapper tradeMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tradeMapper = new TradeMapper();
        tradeService = new TradeService(
                tradeRepository,
                idempotencyService,
                tradeEventPublisher,
                tradeMapper,
                successCounter,
                duplicateCounter,
                failedCounter,
                latencyTimer
        );
    }

    @Test
    void shouldIngestTradeSuccessfully() {
        TradeRequest request = new TradeRequest(
                "TRD-1001",
                "AAPL",
                LocalDate.parse("2026-02-28"),
                LocalDate.parse("2026-03-03"),
                "CPTY-A",
                new BigDecimal("100.000000"),
                new BigDecimal("150.250000"),
                "USD",
                "idem-1001"
        );

        Trade saved = new Trade();
        saved.setId(UUID.randomUUID());
        saved.setTradeId(request.tradeId());
        saved.setInstrument(request.instrument());
        saved.setTradeDate(request.tradeDate());
        saved.setExpectedSettlementDate(request.expectedSettlementDate());
        saved.setCounterparty(request.counterparty());
        saved.setQuantity(request.quantity());
        saved.setPrice(request.price());
        saved.setCurrency(request.currency());
        saved.setStatus(TradeStatus.PENDING);
        saved.setIdempotencyKey(request.idempotencyKey());
        saved.setCreatedAt(Instant.now());
        saved.setUpdatedAt(Instant.now());

        when(idempotencyService.isAlreadyProcessed("idem-1001")).thenReturn(false);
        when(tradeRepository.existsByIdempotencyKey("idem-1001")).thenReturn(false);
        when(tradeRepository.save(any(Trade.class))).thenReturn(saved);
        doNothing().when(tradeEventPublisher).publishTradeEvent(saved);

        TradeDTO response = tradeService.ingestTrade(request);

        assertEquals("TRD-1001", response.tradeId());
        assertEquals(UUID.nameUUIDFromBytes("TRD-1001".getBytes(StandardCharsets.UTF_8)), response.stableTradeId());
        verify(idempotencyService).markAsProcessed("idem-1001");
        verify(successCounter).increment();
    }

    @Test
    void shouldReturnExistingTradeForDuplicateIdempotencyKey() {
        TradeRequest request = new TradeRequest(
                "TRD-2002",
                "MSFT",
                LocalDate.parse("2026-02-28"),
                LocalDate.parse("2026-03-03"),
                "CPTY-B",
                new BigDecimal("10.000000"),
                new BigDecimal("80.000000"),
                "USD",
                "idem-2002"
        );

        Trade existing = new Trade();
        existing.setId(UUID.randomUUID());
        existing.setTradeId(request.tradeId());
        existing.setInstrument(request.instrument());
        existing.setTradeDate(request.tradeDate());
        existing.setExpectedSettlementDate(request.expectedSettlementDate());
        existing.setCounterparty(request.counterparty());
        existing.setQuantity(request.quantity());
        existing.setPrice(request.price());
        existing.setCurrency(request.currency());
        existing.setStatus(TradeStatus.PENDING);
        existing.setIdempotencyKey(request.idempotencyKey());
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        when(idempotencyService.isAlreadyProcessed("idem-2002")).thenReturn(true);
        when(tradeRepository.findByIdempotencyKey("idem-2002")).thenReturn(Optional.of(existing));

        TradeDTO response = tradeService.ingestTrade(request);

        assertNotNull(response);
        assertEquals("TRD-2002", response.tradeId());
        assertNotNull(response.stableTradeId());
        verify(duplicateCounter).increment();
    }

    @Test
    void shouldThrowTradeNotFoundExceptionWhenIdMissing() {
        UUID tradeId = UUID.randomUUID();
        when(tradeRepository.findById(tradeId)).thenReturn(Optional.empty());

        assertThrows(TradeNotFoundException.class, () -> tradeService.getTrade(tradeId));
    }

    @Test
    void shouldListTradesPagedWithoutStatusFilter() {
        Trade trade = new Trade();
        trade.setId(UUID.randomUUID());
        trade.setTradeId("TRD-LIST-1");
        trade.setInstrument("AAPL");
        trade.setTradeDate(LocalDate.parse("2026-02-28"));
        trade.setExpectedSettlementDate(LocalDate.parse("2026-03-03"));
        trade.setCounterparty("CPTY-A");
        trade.setQuantity(new BigDecimal("100.000000"));
        trade.setPrice(new BigDecimal("150.250000"));
        trade.setCurrency("USD");
        trade.setStatus(TradeStatus.PENDING);
        trade.setCreatedAt(Instant.now());
        trade.setUpdatedAt(Instant.now());

        Page<Trade> page = new PageImpl<>(List.of(trade));
        when(tradeRepository.findAll(PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))))
                .thenReturn(page);

        Page<TradeDTO> result = tradeService.listTrades(0, 20, null);

        assertEquals(1, result.getTotalElements());
        assertEquals("TRD-LIST-1", result.getContent().getFirst().tradeId());
    }

    @Test
    void shouldListTradesPagedWithStatusFilter() {
        Trade trade = new Trade();
        trade.setId(UUID.randomUUID());
        trade.setTradeId("TRD-LIST-2");
        trade.setInstrument("MSFT");
        trade.setTradeDate(LocalDate.parse("2026-02-28"));
        trade.setExpectedSettlementDate(LocalDate.parse("2026-03-03"));
        trade.setCounterparty("CPTY-B");
        trade.setQuantity(new BigDecimal("50.000000"));
        trade.setPrice(new BigDecimal("220.100000"));
        trade.setCurrency("USD");
        trade.setStatus(TradeStatus.BREACHED);
        trade.setCreatedAt(Instant.now());
        trade.setUpdatedAt(Instant.now());

        Page<Trade> page = new PageImpl<>(List.of(trade));
        when(tradeRepository.findByStatus(
                org.mockito.ArgumentMatchers.eq(TradeStatus.BREACHED),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class))
        ).thenReturn(page);

        Page<TradeDTO> result = tradeService.listTrades(0, 20, TradeStatus.BREACHED);

        assertEquals(1, result.getTotalElements());
        assertEquals(TradeStatus.BREACHED, result.getContent().getFirst().status());
    }
}
