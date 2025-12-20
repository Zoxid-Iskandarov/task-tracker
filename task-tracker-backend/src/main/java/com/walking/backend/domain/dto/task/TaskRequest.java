package com.walking.backend.domain.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for creating or updating a task")
public record TaskRequest(
        @Schema(
                description = "Title of the task",
                example = "Buy groceries",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Title cannot be empty")
        @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
        String title,

        @Schema(
                description = "Detailed description of what needs to be done",
                example = "Milk, eggs, bread, and some fruits"
        )
        @Size(max = 5000, message = "Description cannot exceed 5000 characters")
        String description) {
}
