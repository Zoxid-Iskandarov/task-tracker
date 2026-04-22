package com.walking.backend.domain.dto.task;

import com.walking.backend.domain.dto.label.LabelResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Response object representing a task")
public record TaskFullResponse(
        @Schema(description = "Unique identifier of the task", example = "1")
        Long id,

        @Schema(description = "Task title", example = "Buy groceries")
        String title,

        @Schema(description = "Task description", example = "Milk, eggs, bread, and some fruits")
        String description,

        @Schema(description = "Completion status flag", example = "false")
        Boolean isCompleted,

        Long sectionId,

        List<LabelResponse> labels,

        @Schema(description = "Task creation timestamp", example = "2025-12-20T10:00:00")
        LocalDateTime created,

        @Schema(description = "Last update timestamp", example = "2025-12-20T12:30:00")
        LocalDateTime updated) {
}
