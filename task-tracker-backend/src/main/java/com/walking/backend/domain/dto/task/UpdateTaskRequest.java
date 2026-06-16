package com.walking.backend.domain.dto.task;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;

public record UpdateTaskRequest(
        @NotBlank(message = "Title cannot be empty")
        @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
        String title,

        @Size(max = 5000, message = "Description cannot exceed 5000 characters")
        String description,

        @FutureOrPresent(message = "Due date must be in the present or future")
        LocalDateTime dueDate,

        @Size(max = 20, message = "Task cannot have more than 20 assignees")
        Set<@Positive(message = "Assignee id must be positive") Long> assigneeIds) {
}
