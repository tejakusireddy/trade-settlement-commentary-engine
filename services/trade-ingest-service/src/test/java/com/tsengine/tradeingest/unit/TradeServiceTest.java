package com.tsengine.tradeingest.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tsengine.common.DuplicateTradeException;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
        verify(idempotencyService).markAsProcessed("idem-1001");
        verify(successCounter).increment();
    }

    @Test
    void shouldThrowDuplicateTradeException() {
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

        when(idempotencyService.isAlreadyProcessed("idem-2002")).thenReturn(true);

        assertThrows(DuplicateTradeException.class, () -> tradeService.ingestTrade(request));
        verify(duplicateCounter).increment();
    }

    @Test
    void shouldThrowTradeNotFoundExceptionWhenIdMissing() {
        UUID tradeId = UUID.randomUUID();
        when(tradeRepository.findById(tradeId)).thenReturn(Optional.empty());

        assertThrows(TradeNotFoundException.class, () -> tradeService.getTrade(tradeId));
    }
}
