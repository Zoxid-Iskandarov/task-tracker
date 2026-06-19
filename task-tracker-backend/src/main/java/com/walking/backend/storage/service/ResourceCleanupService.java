package com.walking.backend.storage.service;

import com.walking.backend.domain.event.FileCleanupEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceCleanupService {
    private final ApplicationEventPublisher eventPublisher;

    public void cleanupFiles(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return;
        }

        eventPublisher.publishEvent(new FileCleanupEvent(filePaths));
    }
}
