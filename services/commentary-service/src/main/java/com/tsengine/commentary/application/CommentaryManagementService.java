package com.tsengine.commentary.application;

import com.tsengine.commentary.domain.Commentary;
import com.tsengine.commentary.exception.CommentaryNotFoundException;
import com.tsengine.commentary.infrastructure.CommentaryJpaRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentaryManagementService {

    private final CommentaryJpaRepository commentaryJpaRepository;

    public CommentaryManagementService(CommentaryJpaRepository commentaryJpaRepository) {
        this.commentaryJpaRepository = commentaryJpaRepository;
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
        return commentaryJpaRepository.save(commentary);
    }
}
