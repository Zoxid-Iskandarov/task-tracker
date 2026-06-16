package com.walking.backend.domain.dto.user;

public record UserShortResponse(
        Long id,
        String username,
        String displayName,
        String avatarUrl) {
}
