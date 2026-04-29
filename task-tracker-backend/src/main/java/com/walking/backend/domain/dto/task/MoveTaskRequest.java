package com.walking.backend.domain.dto.task;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MoveTaskRequest(
        @NotNull(message = "Section id cannot be null")
        @Positive(message = "Section id must be positive")
        Long sectionId,

        @Positive(message = "Previous task id must be positive")
        Long prevTaskId,

        @Positive(message = "Next task id must be positive")
        Long nextTaskId) {
}
