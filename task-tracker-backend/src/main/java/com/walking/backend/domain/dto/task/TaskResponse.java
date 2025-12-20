package com.walking.backend.domain.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response object representing a task")
public record TaskResponse(
        @Schema(description = "Unique identifier of the task", example = "1")
        Long id,

        @Schema(description = "Task title", example = "Buy groceries")
        String title,

        @Schema(description = "Task description", example = "Milk, eggs, bread, and some fruits")
        String description,

        @Schema(description = "Completion status flag", example = "false")
        Boolean isCompleted,

        @Schema(description = "Task creation timestamp", example = "2025-12-20T10:00:00")
        LocalDateTime created,

        @Schema(description = "Last update timestamp", example = "2025-12-20T12:30:00")
        LocalDateTime updated) {
}
