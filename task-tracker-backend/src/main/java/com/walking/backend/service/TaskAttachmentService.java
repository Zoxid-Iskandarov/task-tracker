package com.walking.backend.service;

import com.walking.backend.domain.dto.attachment.TaskAttachmentDownloadResponse;
import com.walking.backend.domain.dto.attachment.TaskAttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TaskAttachmentService {

    List<TaskAttachmentResponse> getAttachments(Long taskId);

    TaskAttachmentDownloadResponse getDownloadAttachment(Long taskId, Long attachmentId);

    TaskAttachmentResponse uploadAttachment(Long taskId, Long userId, MultipartFile file);

    void deleteAttachment(Long taskId, Long attachmentId);
}
