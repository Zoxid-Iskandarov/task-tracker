package com.walking.backend.domain.dto.task;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Boolean isCompleted,
        LocalDateTime created,
        LocalDateTime updated) {
}
