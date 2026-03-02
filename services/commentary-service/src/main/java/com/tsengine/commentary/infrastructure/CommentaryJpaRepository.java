package com.tsengine.commentary.infrastructure;

import com.tsengine.commentary.domain.Commentary;
import com.tsengine.commentary.domain.CommentaryRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class CommentaryJpaRepository {

    private final CommentaryRepository commentaryRepository;

    public CommentaryJpaRepository(CommentaryRepository commentaryRepository) {
        this.commentaryRepository = commentaryRepository;
    }

    public Commentary save(Commentary commentary) {
        return commentaryRepository.save(commentary);
    }

    public Page<Commentary> findAll(Pageable pageable) {
        return commentaryRepository.findAll(pageable);
    }

    public Optional<Commentary> findById(UUID id) {
        return commentaryRepository.findById(id);
    }

    public Optional<Commentary> findByBreachId(UUID breachId) {
        return commentaryRepository.findByBreachId(breachId);
    }
}
