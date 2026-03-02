package com.tsengine.breachdetector.unit;

import static org.mockito.Mockito.verify;

import com.tsengine.breachdetector.application.BreachStatusService;
import com.tsengine.breachdetector.infrastructure.KafkaCommentaryStatusConsumer;
import com.tsengine.schema.CommentaryApproved;
import com.tsengine.schema.CommentaryCompleted;
import com.tsengine.schema.DlqEvent;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

class KafkaCommentaryStatusConsumerTest {

    @Mock
    private BreachStatusService breachStatusService;
    @Mock
    private KafkaTemplate<String, DlqEvent> dlqKafkaTemplate;
    @Mock
    private Counter eventsSentToDlqCounter;
    @Mock
    private Acknowledgment acknowledgment;

    private KafkaCommentaryStatusConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new KafkaCommentaryStatusConsumer(
                breachStatusService,
                dlqKafkaTemplate,
                eventsSentToDlqCounter
        );
    }

    @Test
    void shouldAdvanceStatusOnCommentaryCompleted() {
        UUID breachId = UUID.randomUUID();
        CommentaryCompleted event = CommentaryCompleted.newBuilder()
                .setCommentaryId(UUID.randomUUID().toString())
                .setBreachId(breachId.toString())
                .setTradeId(UUID.randomUUID().toString())
                .setContent("content")
                .setGenerationType("AI")
                .setPromptVersion("v1")
                .setCostUsd(0.001)
                .setTokensInput(10)
                .setTokensOutput(20)
                .setCompletedAt(Instant.now())
                .build();

        consumer.consumeCompleted(event, acknowledgment, "commentary.completed");

        verify(breachStatusService).markCommentaryGenerated(breachId);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldAdvanceStatusOnCommentaryApproved() {
        UUID breachId = UUID.randomUUID();
        CommentaryApproved event = CommentaryApproved.newBuilder()
                .setCommentaryId(UUID.randomUUID().toString())
                .setBreachId(breachId.toString())
                .setTradeId(UUID.randomUUID().toString())
                .setApprovedBy("admin-test")
                .setApprovedAt(Instant.now())
                .build();

        consumer.consumeApproved(event, acknowledgment, "commentary.approved");

        verify(breachStatusService).markCommentaryApproved(breachId);
        verify(acknowledgment).acknowledge();
    }
}
