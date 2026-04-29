package com.walking.backend.domain.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserSearchFilter(
        @NotBlank(message = "Query cannot be empty")
        @Size(min = 3, message = "Query is too short")
        String query) {
}
