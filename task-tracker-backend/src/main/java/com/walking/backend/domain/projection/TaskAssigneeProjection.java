package com.walking.backend.domain.projection;

public record TaskAssigneeProjection(
        Long taskId,
        Long userId,
        String username,
        String displayName,
        String avatarUrl) {
}
