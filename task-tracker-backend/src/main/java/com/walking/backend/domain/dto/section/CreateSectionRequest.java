package com.walking.backend.domain.dto.section;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSectionRequest(
        @NotBlank(message = "Section name cannot be empty")
        @Size(min = 1, max = 100, message = "Section name must be between 1 and 100 characters")
        String name,

        @NotNull(message = "Board id cannot be null")
        @Positive(message = "Board id must be positive")
        Long boardId) {
}
