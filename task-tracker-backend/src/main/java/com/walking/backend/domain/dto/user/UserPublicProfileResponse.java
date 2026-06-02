package com.walking.backend.domain.dto.user;

public record UserPublicProfileResponse(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        String bio) {
}
