package com.tsengine.commentary.api;

import com.tsengine.commentary.domain.Commentary;
import com.tsengine.common.CommentaryDTO;
import org.springframework.stereotype.Component;

@Component
public class CommentaryMapper {

    public CommentaryDTO toDto(Commentary commentary) {
        return new CommentaryDTO(
                commentary.getId(),
                commentary.getBreachId(),
                commentary.getContent(),
                commentary.getGenerationType(),
                commentary.getPromptVersion(),
                commentary.getApprovedBy(),
                commentary.getApprovedAt(),
                commentary.getCreatedAt()
        );
    }
}
