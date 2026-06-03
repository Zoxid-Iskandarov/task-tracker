package com.walking.backend.domain.dto.user;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String avatarUrl,
        String bio) {
}
