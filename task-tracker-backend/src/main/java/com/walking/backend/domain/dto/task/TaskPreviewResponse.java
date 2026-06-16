package com.walking.backend.domain.dto.task;

import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.dto.user.UserShortResponse;

import java.time.LocalDateTime;
import java.util.List;

public record TaskPreviewResponse(
        Long id,
        String title,
        Boolean isCompleted,
        LocalDateTime dueDate,
        Long sectionId,
        List<LabelResponse> labels,
        List<UserShortResponse> assignees,
        LocalDateTime created,
        LocalDateTime updated
) {
}
