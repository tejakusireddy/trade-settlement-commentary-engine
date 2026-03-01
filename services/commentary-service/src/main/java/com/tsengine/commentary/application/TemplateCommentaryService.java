package com.tsengine.commentary.application;

import com.tsengine.schema.BreachEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TemplateCommentaryService {

    public String generateCommentary(BreachEvent event) {
        String template;
        switch (event.getBreachReason()) {
            case "MISSING_ASSIGNMENT" -> template =
                    "Trade %s for %s with %s has breached %s settlement deadline by %d business days due "
                            + "to missing assignment. Immediate assignment resolution required to prevent "
                            + "further settlement failures.";
            case "FAILED_ALLOCATION" -> template =
                    "Settlement failure for %s trade %s with %s (%d days overdue). Root cause: failed "
                            + "allocation. Operations team has been notified and is working to resolve the "
                            + "allocation issue.";
            case "COUNTERPARTY_FAILURE" -> template =
                    "Trade %s (%s) remains unsettled %d business days past %s deadline. Counterparty %s has "
                            + "failed to deliver. Escalation to counterparty relationship management initiated.";
            case "INSUFFICIENT_FUNDS" -> template =
                    "Settlement breach on %s trade %s (%d days overdue). Insufficient funds identified at %s. "
                            + "Risk management has been alerted and collateral review is underway.";
            default -> template =
                    "Technical settlement failure for trade %s (%s) with %s. Breach classification: %s, %d "
                            + "days overdue. IT operations team investigating system issue. Manual settlement "
                            + "process initiated as contingency.";
        }

        return switch (event.getBreachReason()) {
            case "FAILED_ALLOCATION" -> String.format(
                    template,
                    event.getInstrument(),
                    event.getTradeId(),
                    event.getCounterparty(),
                    event.getDaysOverdue()
            );
            case "COUNTERPARTY_FAILURE" -> String.format(
                    template,
                    event.getTradeId(),
                    event.getInstrument(),
                    event.getDaysOverdue(),
                    event.getBreachType(),
                    event.getCounterparty()
            );
            case "INSUFFICIENT_FUNDS" -> String.format(
                    template,
                    event.getInstrument(),
                    event.getTradeId(),
                    event.getDaysOverdue(),
                    event.getCounterparty()
            );
            default -> String.format(
                    template,
                    event.getTradeId(),
                    event.getInstrument(),
                    event.getCounterparty(),
                    event.getBreachType(),
                    event.getDaysOverdue()
            );
        };
    }
}
