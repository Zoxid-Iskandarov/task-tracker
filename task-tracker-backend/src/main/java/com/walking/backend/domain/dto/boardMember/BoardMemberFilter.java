package com.walking.backend.domain.dto.boardMember;

import com.walking.backend.domain.model.BoardRole;

import java.time.LocalDateTime;

public record BoardMemberFilter(
        String username,
        String email,
        BoardRole role,
        LocalDateTime joinedFrom,
        LocalDateTime joinedTo) {
}
