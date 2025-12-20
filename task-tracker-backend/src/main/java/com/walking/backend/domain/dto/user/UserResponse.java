package com.walking.backend.domain.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data transfer object for user profile information")
public record UserResponse(
        @Schema(description = "Unique user identifier", example = "1")
        Long id,

        @Schema(description = "User's unique display name", example = "johndoe")
        String username,

        @Schema(description = "User's contact email address", example = "john@example.com")
        String email) {
}
