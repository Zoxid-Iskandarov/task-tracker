package com.walking.backend.domain.dto.label;

public record CreateLabelRequest(String name, String colour, Long boardId) {
}
