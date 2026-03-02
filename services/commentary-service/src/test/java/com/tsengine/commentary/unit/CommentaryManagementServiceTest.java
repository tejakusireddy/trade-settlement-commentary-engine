package com.tsengine.commentary.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tsengine.commentary.application.CommentaryManagementService;
import com.tsengine.commentary.config.KafkaTopicsProperties;
import com.tsengine.commentary.domain.Commentary;
import com.tsengine.commentary.exception.CommentaryNotFoundException;
import com.tsengine.commentary.infrastructure.CommentaryJpaRepository;
import com.tsengine.commentary.infrastructure.KafkaCommentaryProducer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommentaryManagementServiceTest {

    @Test
    void shouldApproveAndPublishApprovalEvent() {
        UUID commentaryId = UUID.randomUUID();
        Commentary commentary = new Commentary();
        commentary.setId(commentaryId);
        commentary.setBreachId(UUID.randomUUID());
        commentary.setTradeId(UUID.randomUUID());
        commentary.setContent("sample");
        commentary.setPromptVersion("v1");
        commentary.setCreatedAt(Instant.now());

        InMemoryCommentaryJpaRepository repository = new InMemoryCommentaryJpaRepository();
        repository.store(commentary);
        CapturingKafkaCommentaryProducer producer = new CapturingKafkaCommentaryProducer();

        CommentaryManagementService service = new CommentaryManagementService(repository, producer);
        Commentary approved = service.approve(commentaryId, "admin-test");

        assertEquals("admin-test", approved.getApprovedBy());
        assertNotNull(approved.getApprovedAt());
        assertEquals(commentaryId, producer.lastPublishedApproval.getId());
    }

    @Test
    void shouldThrowWhenCommentaryMissing() {
        InMemoryCommentaryJpaRepository repository = new InMemoryCommentaryJpaRepository();
        CapturingKafkaCommentaryProducer producer = new CapturingKafkaCommentaryProducer();
        CommentaryManagementService service = new CommentaryManagementService(repository, producer);

        assertThrows(CommentaryNotFoundException.class, () -> service.approve(UUID.randomUUID(), "admin-test"));
    }

    @Test
    void shouldNotFailApprovalWhenPublishFails() {
        UUID commentaryId = UUID.randomUUID();
        Commentary commentary = new Commentary();
        commentary.setId(commentaryId);
        commentary.setBreachId(UUID.randomUUID());
        commentary.setTradeId(UUID.randomUUID());
        commentary.setContent("sample");
        commentary.setPromptVersion("v1");
        commentary.setCreatedAt(Instant.now());

        InMemoryCommentaryJpaRepository repository = new InMemoryCommentaryJpaRepository();
        repository.store(commentary);
        CommentaryManagementService service = new CommentaryManagementService(
                repository,
                new FailingKafkaCommentaryProducer()
        );

        Commentary approved = service.approve(commentaryId, "admin-test");
        assertEquals("admin-test", approved.getApprovedBy());
        assertNotNull(approved.getApprovedAt());
    }

    private static final class InMemoryCommentaryJpaRepository extends CommentaryJpaRepository {
        private final Map<UUID, Commentary> store = new HashMap<>();

        private InMemoryCommentaryJpaRepository() {
            super(null);
        }

        private void store(Commentary commentary) {
            store.put(commentary.getId(), commentary);
        }

        @Override
        public Optional<Commentary> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Commentary save(Commentary commentary) {
            store.put(commentary.getId(), commentary);
            return commentary;
        }
    }

    private static final class CapturingKafkaCommentaryProducer extends KafkaCommentaryProducer {
        private Commentary lastPublishedApproval;

        private CapturingKafkaCommentaryProducer() {
            super(null, null, new KafkaTopicsProperties());
        }

        @Override
        public void publishCommentaryCompleted(Commentary commentary, double costUsd, int tokensInput, int tokensOutput) {
            // no-op
        }

        @Override
        public void publishCommentaryApproved(Commentary commentary) {
            lastPublishedApproval = commentary;
        }
    }

    private static final class FailingKafkaCommentaryProducer extends KafkaCommentaryProducer {
        private FailingKafkaCommentaryProducer() {
            super(null, null, new KafkaTopicsProperties());
        }

        @Override
        public void publishCommentaryCompleted(Commentary commentary, double costUsd, int tokensInput, int tokensOutput) {
            // no-op
        }

        @Override
        public void publishCommentaryApproved(Commentary commentary) {
            throw new RuntimeException("kafka unavailable");
        }
    }
}
