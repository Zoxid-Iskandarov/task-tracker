package com.walking.backend.domain.dto.boardMember;

import com.walking.backend.domain.model.BoardRole;

public record BoardMemberRequest(Long userId, BoardRole role) {
}
