package com.walking.backend.domain.dto.board;

import java.time.LocalDateTime;

public record BoardResponse(Long id, String name, LocalDateTime created, LocalDateTime updated) {
}
