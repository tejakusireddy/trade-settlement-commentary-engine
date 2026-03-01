package com.tsengine.commentary.api;

import jakarta.validation.constraints.NotBlank;

public record ApproveCommentaryRequest(@NotBlank String approvedBy) {
}
