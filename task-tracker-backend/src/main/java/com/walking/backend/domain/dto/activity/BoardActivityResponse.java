package com.walking.backend.domain.dto.activity;

import com.walking.backend.domain.model.ActivityType;

import java.time.LocalDateTime;

public record BoardActivityResponse(
        Long userId,
        String username,
        String displayName,
        String avatarUrl,
        ActivityType activityType,
        String description,
        LocalDateTime created) {
}
