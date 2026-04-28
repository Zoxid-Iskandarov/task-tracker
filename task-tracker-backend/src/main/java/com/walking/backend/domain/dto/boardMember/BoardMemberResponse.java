package com.walking.backend.domain.dto.boardMember;

import com.walking.backend.domain.model.BoardRole;

import java.time.LocalDateTime;

public record BoardMemberResponse(
        Long userId,
        String username,
        String email,
        BoardRole role,
        LocalDateTime joined) {
}
