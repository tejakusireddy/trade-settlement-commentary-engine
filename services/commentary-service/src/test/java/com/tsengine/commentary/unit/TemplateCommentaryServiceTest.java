package com.tsengine.commentary.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tsengine.commentary.application.TemplateCommentaryService;
import com.tsengine.schema.BreachEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TemplateCommentaryServiceTest {

    private final TemplateCommentaryService service = new TemplateCommentaryService();

    @Test
    void testMissingAssignmentTemplate() {
        String content = service.generateCommentary(eventWithReason("MISSING_ASSIGNMENT"));
        assertTrue(content.contains("missing assignment"));
    }

    @Test
    void testCounterpartyFailureTemplate() {
        String content = service.generateCommentary(eventWithReason("COUNTERPARTY_FAILURE"));
        assertTrue(content.contains("Counterparty"));
    }

    @Test
    void testAllBranchesResolvePlaceholders() {
        List<String> reasons = List.of(
                "MISSING_ASSIGNMENT",
                "FAILED_ALLOCATION",
                "COUNTERPARTY_FAILURE",
                "INSUFFICIENT_FUNDS",
                "SYSTEM_ERROR"
        );

        for (String reason : reasons) {
            String content = service.generateCommentary(eventWithReason(reason));
            assertFalse(content.contains("{"));
            assertFalse(content.contains("}"));
        }
    }

    private BreachEvent eventWithReason(String reason) {
        return BreachEvent.newBuilder()
                .setBreachId(UUID.randomUUID().toString())
                .setTradeId("TRD-123")
                .setInstrument("AAPL")
                .setCounterparty("CP-A")
                .setBreachType("T3")
                .setBreachReason(reason)
                .setDaysOverdue(3)
                .setTradeDate("2026-03-01")
                .setDetectedAt(System.currentTimeMillis())
                .build();
    }
}
