package com.tsengine.breachdetector.unit;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tsengine.breachdetector.application.BreachStatusService;
import com.tsengine.breachdetector.domain.Breach;
import com.tsengine.breachdetector.domain.BreachRepository;
import com.tsengine.breachdetector.domain.BreachStatuses;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BreachStatusServiceTest {

    @Mock
    private BreachRepository breachRepository;

    private BreachStatusService breachStatusService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        breachStatusService = new BreachStatusService(breachRepository);
    }

    @Test
    void shouldMarkCommentaryGeneratedFromPending() {
        UUID breachId = UUID.randomUUID();
        Breach breach = new Breach();
        breach.setId(breachId);
        breach.setStatus(BreachStatuses.PENDING_COMMENTARY);
        when(breachRepository.findById(breachId)).thenReturn(Optional.of(breach));

        breachStatusService.markCommentaryGenerated(breachId);

        verify(breachRepository).save(breach);
        org.junit.jupiter.api.Assertions.assertEquals(BreachStatuses.COMMENTARY_GENERATED, breach.getStatus());
    }

    @Test
    void shouldNotDowngradeApprovedStatusWhenGeneratedEventArrivesLate() {
        UUID breachId = UUID.randomUUID();
        Breach breach = new Breach();
        breach.setId(breachId);
        breach.setStatus(BreachStatuses.COMMENTARY_APPROVED);
        when(breachRepository.findById(breachId)).thenReturn(Optional.of(breach));

        breachStatusService.markCommentaryGenerated(breachId);

        verify(breachRepository, never()).save(breach);
    }

    @Test
    void shouldMarkCommentaryApproved() {
        UUID breachId = UUID.randomUUID();
        Breach breach = new Breach();
        breach.setId(breachId);
        breach.setStatus(BreachStatuses.COMMENTARY_GENERATED);
        when(breachRepository.findById(breachId)).thenReturn(Optional.of(breach));

        breachStatusService.markCommentaryApproved(breachId);

        verify(breachRepository).save(breach);
        org.junit.jupiter.api.Assertions.assertEquals(BreachStatuses.COMMENTARY_APPROVED, breach.getStatus());
    }

    @Test
    void shouldIgnoreUnknownBreachIds() {
        UUID breachId = UUID.randomUUID();
        when(breachRepository.findById(breachId)).thenReturn(Optional.empty());

        breachStatusService.markCommentaryApproved(breachId);

        verify(breachRepository, never()).save(org.mockito.ArgumentMatchers.any(Breach.class));
    }
}
