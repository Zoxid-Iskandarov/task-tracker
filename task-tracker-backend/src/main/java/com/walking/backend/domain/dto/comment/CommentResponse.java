package com.walking.backend.domain.dto.comment;

import com.walking.backend.domain.dto.user.UserShortResponse;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String content,
        UserShortResponse author,
        boolean isEdited,
        LocalDateTime created,
        LocalDateTime updated) {
}
