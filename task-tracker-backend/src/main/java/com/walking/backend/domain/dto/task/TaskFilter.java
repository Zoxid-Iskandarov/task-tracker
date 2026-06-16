package com.walking.backend.domain.dto.task;

import java.time.LocalDateTime;
import java.util.List;

public record TaskFilter(
        String title,
        Long sectionId,
        Boolean completed,
        List<Long> labelIds,
        List<Long> assigneeIds,
        LocalDateTime dueDateFrom,
        LocalDateTime dueDateTo,
        LocalDateTime createdFrom,
        LocalDateTime createdTo) {
}
