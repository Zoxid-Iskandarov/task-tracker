package com.walking.backend.domain.dto.boardMember;

import com.walking.backend.domain.model.BoardRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BoardMemberRequest(
        @NotNull(message = "User id cannot be null")
        @Positive(message = "User id must be positive")
        Long userId,

        @NotNull(message = "Role cannot be null")
        BoardRole role) {
}
