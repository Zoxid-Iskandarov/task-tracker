package com.walking.backend.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Credentials for user authentication")
public record SignInRequest(
        @Schema(description = "Registered username", example = "johndoe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 3, max = 100, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
        String username,

        @Schema(description = "User password", example = "Password123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Password cannot be empty")
        @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$",
                message = "Password must contain at least one digit, one lowercase letter, and one uppercase letter")
        String password) {
}
