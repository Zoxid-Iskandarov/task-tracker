package com.walking.backend.domain.dto.task;

import com.walking.backend.domain.dto.label.LabelResponse;

import java.time.LocalDateTime;
import java.util.List;

public record TaskPreviewResponse(
        Long id,
        String title,
        Boolean isCompleted,
        Long sectionId,
        List<LabelResponse> labels,
        LocalDateTime created,
        LocalDateTime updated
) {
}
