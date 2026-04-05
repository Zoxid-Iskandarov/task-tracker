package com.walking.backend.domain.dto.section;

import java.time.LocalDateTime;

public record SectionResponse(Long id, String name, Long boardId, LocalDateTime created, LocalDateTime updated) {
}
