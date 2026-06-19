package com.walking.backend.domain.dto.attachment;

public record TaskAttachmentDownloadResponse(
        String url,
        String fileName,
        String contentType) {
}
