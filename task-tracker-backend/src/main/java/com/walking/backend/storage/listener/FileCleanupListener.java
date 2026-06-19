package com.walking.backend.storage.listener;

import com.walking.backend.domain.event.FileCleanupEvent;
import com.walking.backend.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class FileCleanupListener {
    private final FileStorageService fileStorageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFileCleanupEvent(FileCleanupEvent event) {
        fileStorageService.deleteAttachments(event.filePaths());
    }
}
