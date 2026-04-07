package com.walking.backend.domain.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response containing JWT tokens")
public record AuthResponse(
        @Schema(description = "Short-lived JWT access token for resource access", example = "eyJhbGci...")
        @JsonProperty("access_token")
        String accessToken) {
}
