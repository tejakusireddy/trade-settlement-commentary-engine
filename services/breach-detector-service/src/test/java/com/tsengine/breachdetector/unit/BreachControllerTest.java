package com.tsengine.breachdetector.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.tsengine.breachdetector.api.BreachController;
import com.tsengine.breachdetector.api.BreachMapper;
import com.tsengine.breachdetector.api.BreachResponse;
import com.tsengine.breachdetector.domain.Breach;
import com.tsengine.breachdetector.domain.BreachRepository;
import com.tsengine.common.BreachReason;
import com.tsengine.common.BreachType;
import java.time.Instant;
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
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

class BreachControllerTest {

    @Mock
    private BreachRepository breachRepository;

    private BreachController breachController;
    private BreachMapper breachMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        breachMapper = new BreachMapper();
        breachController = new BreachController(breachRepository, breachMapper);
    }

    @Test
    void shouldReturnPagedBreaches() {
        Breach breach = buildBreach();
        Page<Breach> page = new PageImpl<>(List.of(breach));
        when(breachRepository.findAll(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "detectedAt"))))
                .thenReturn(page);

        Page<BreachResponse> result = breachController.listBreaches(0, 20).data();

        assertEquals(1, result.getTotalElements());
        assertEquals(breach.getId(), result.getContent().getFirst().id());
    }

    @Test
    void shouldReturnBreachById() {
        Breach breach = buildBreach();
        when(breachRepository.findById(breach.getId())).thenReturn(Optional.of(breach));

        BreachResponse result = breachController.getById(breach.getId()).data();

        assertEquals(breach.getId(), result.id());
        assertEquals("PENDING_COMMENTARY", result.status());
    }

    @Test
    void shouldThrowNotFoundForMissingBreach() {
        UUID id = UUID.randomUUID();
        when(breachRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> breachController.getById(id));
    }

    private Breach buildBreach() {
        Breach breach = new Breach();
        breach.setId(UUID.randomUUID());
        breach.setTradeId(UUID.randomUUID());
        breach.setInstrument("AAPL");
        breach.setCounterparty("CPTY-A");
        breach.setBreachType(BreachType.T2);
        breach.setBreachReason(BreachReason.MISSING_ASSIGNMENT);
        breach.setDaysOverdue(2);
        breach.setDetectedAt(Instant.now());
        breach.setStatus("PENDING_COMMENTARY");
        return breach;
    }

}
