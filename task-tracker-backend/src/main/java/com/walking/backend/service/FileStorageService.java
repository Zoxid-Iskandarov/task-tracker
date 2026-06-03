package com.walking.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String upload(Long userId, MultipartFile file);

    void delete(String objectName);
}
