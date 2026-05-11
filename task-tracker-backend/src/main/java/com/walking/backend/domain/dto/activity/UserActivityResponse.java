package com.walking.backend.domain.dto.activity;

import com.walking.backend.domain.model.ActivityType;

import java.time.LocalDateTime;

public record UserActivityResponse(
        Long boardId,
        String boardName,
        ActivityType activityType,
        String description,
        LocalDateTime created) {
}
