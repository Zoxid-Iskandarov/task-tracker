package com.walking.backend.domain.event;

import java.util.List;

public record FileCleanupEvent(List<String> filePaths) {
}
