package com.walking.backend.domain.dto.activity;

import com.walking.backend.domain.model.ActivityType;

public record UserActivityInternalEvent(
        Long userId,
        String username,
        String email,
        Long boardId,
        String boardName,
        ActivityType type,
        String description) {
}
