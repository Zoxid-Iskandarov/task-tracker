package com.walking.backend.domain.dto.label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateLabelRequest(
        @NotBlank(message = "Label name cannot be empty")
        @Size(min = 1, max = 50, message = "Label name must be between 1 and 50 characters")
        String name,

        @NotBlank(message = "Colour cannot be empty")
        @Pattern(
                regexp = "^#([A-Fa-f0-9]{6})$",
                message = "Colour must be a valid HEX value (e.g. #A1B2C3)"
        )
        String colour) {
}
