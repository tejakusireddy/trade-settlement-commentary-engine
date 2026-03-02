package com.tsengine.breachdetector.application;

import com.tsengine.breachdetector.domain.BreachRepository;
import com.tsengine.breachdetector.domain.BreachStatuses;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BreachStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BreachStatusService.class);

    private final BreachRepository breachRepository;

    public BreachStatusService(BreachRepository breachRepository) {
        this.breachRepository = breachRepository;
    }

    @Transactional
    public void markCommentaryGenerated(UUID breachId) {
        breachRepository.findById(breachId).ifPresentOrElse(breach -> {
            String currentStatus = breach.getStatus();
            if (BreachStatuses.COMMENTARY_APPROVED.equals(currentStatus)
                    || BreachStatuses.COMMENTARY_GENERATED.equals(currentStatus)) {
                return;
            }
            breach.setStatus(BreachStatuses.COMMENTARY_GENERATED);
            breachRepository.save(breach);
        }, () -> LOGGER.warn("Ignoring commentary.completed for unknown breachId={}", breachId));
    }

    @Transactional
    public void markCommentaryApproved(UUID breachId) {
        breachRepository.findById(breachId).ifPresentOrElse(breach -> {
            if (BreachStatuses.COMMENTARY_APPROVED.equals(breach.getStatus())) {
                return;
            }
            breach.setStatus(BreachStatuses.COMMENTARY_APPROVED);
            breachRepository.save(breach);
        }, () -> LOGGER.warn("Ignoring commentary.approved for unknown breachId={}", breachId));
    }
}
