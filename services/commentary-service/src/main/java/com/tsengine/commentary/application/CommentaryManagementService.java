package com.tsengine.commentary.application;

import com.tsengine.commentary.domain.Commentary;
import com.tsengine.commentary.exception.CommentaryNotFoundException;
import com.tsengine.commentary.infrastructure.CommentaryJpaRepository;
import com.tsengine.commentary.infrastructure.KafkaCommentaryProducer;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentaryManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommentaryManagementService.class);

    private final CommentaryJpaRepository commentaryJpaRepository;
    private final KafkaCommentaryProducer kafkaCommentaryProducer;

    public CommentaryManagementService(
            CommentaryJpaRepository commentaryJpaRepository,
            KafkaCommentaryProducer kafkaCommentaryProducer
    ) {
        this.commentaryJpaRepository = commentaryJpaRepository;
        this.kafkaCommentaryProducer = kafkaCommentaryProducer;
    }

    @Transactional(readOnly = true)
    public Page<Commentary> listAll(Pageable pageable) {
        return commentaryJpaRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Commentary getById(UUID id) {
        return commentaryJpaRepository.findById(id)
                .orElseThrow(() -> new CommentaryNotFoundException("Commentary not found for id=" + id));
    }

    @Transactional(readOnly = true)
    public Commentary getByBreachId(UUID breachId) {
        return commentaryJpaRepository.findByBreachId(breachId)
                .orElseThrow(() -> new CommentaryNotFoundException("Commentary not found for breachId=" + breachId));
    }

    @Transactional
    public Commentary approve(UUID commentaryId, String approvedBy) {
        Commentary commentary = getById(commentaryId);
        commentary.setApprovedBy(approvedBy);
        commentary.setApprovedAt(Instant.now());
        Commentary saved = commentaryJpaRepository.save(commentary);
        try {
            kafkaCommentaryProducer.publishCommentaryApproved(saved);
        } catch (RuntimeException ex) {
            LOGGER.error("Failed to publish commentary.approved event commentaryId={}", commentaryId, ex);
        }
        return saved;
    }
}
