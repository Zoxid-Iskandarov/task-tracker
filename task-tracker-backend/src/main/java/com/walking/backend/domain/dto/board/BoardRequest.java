package com.walking.backend.domain.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardRequest(
        @NotBlank(message = "Board name cannot be empty")
        @Size(min = 3, max = 100, message = "Board name must be between 3 and 100 characters")
        String name) {
}
