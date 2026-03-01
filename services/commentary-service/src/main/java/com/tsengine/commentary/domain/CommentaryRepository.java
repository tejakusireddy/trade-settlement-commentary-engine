package com.tsengine.commentary.domain;

import com.tsengine.common.CommentaryGenerationType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentaryRepository extends JpaRepository<Commentary, UUID> {

    Optional<Commentary> findByBreachId(UUID breachId);

    List<Commentary> findByGenerationType(CommentaryGenerationType type);
}
