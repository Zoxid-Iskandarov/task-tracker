package com.walking.backend.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Data required to register a new user account")
public record SignUpRequest(
        @Schema(description = "Desired unique username", example = "johndoe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 3, max = 100, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
        String username,

        @Schema(description = "Valid email address for notifications", example = "john@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Invalid email format")
        String email,

        @Schema(
                description = "User password (must include at least one digit, one uppercase and one lowercase letter)",
                example = "Password123",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Password cannot be empty")
        @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$",
                message = "Password must contain at least one digit, one lowercase letter, and one uppercase letter")
        String password) {
}
