package com.walking.backend.domain.dto.attachment;

import com.walking.backend.domain.dto.user.UserShortResponse;

import java.time.LocalDateTime;

public record TaskAttachmentResponse(
        Long id,
        String fileName,
        String contentType,
        Long fileSize,
        UserShortResponse uploadedBy,
        LocalDateTime created) {
}
