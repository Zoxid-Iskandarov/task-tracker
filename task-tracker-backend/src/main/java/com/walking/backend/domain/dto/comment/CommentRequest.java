package com.walking.backend.domain.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Comment content cannot be empty")
        @Size(min = 1, max = 5000, message = "Comment must be between 1 and 5000 characters")
        String content) {
}
