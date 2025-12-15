package com.walking.backend.domain.dto.user;

public record UserResponse(
        Long id,
        String username,
        String email) {
}
