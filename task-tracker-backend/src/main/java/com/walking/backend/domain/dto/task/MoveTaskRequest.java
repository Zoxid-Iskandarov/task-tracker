package com.walking.backend.domain.dto.task;

public record MoveTaskRequest(Long sectionId, Long prevTaskId, Long nextTaskId) {
}
