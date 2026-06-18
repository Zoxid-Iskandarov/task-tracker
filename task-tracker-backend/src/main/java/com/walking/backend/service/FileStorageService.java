package com.walking.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String uploadAvatar(Long userId, MultipartFile file);

    String uploadAttachment(Long taskId, MultipartFile file);

    void deleteAvatar(String objectName);

    void deleteAttachment(String objectName);

    String generatePresignedUrl(String objectName);
}
