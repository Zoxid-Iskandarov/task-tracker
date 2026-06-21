package com.walking.backend.service.impl;

import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.attachment.TaskAttachmentDownloadResponse;
import com.walking.backend.domain.dto.attachment.TaskAttachmentResponse;
import com.walking.backend.domain.dto.user.UserShortResponse;
import com.walking.backend.domain.exception.AttachmentLimitExceededException;
import com.walking.backend.domain.exception.IllegalOperationException;
import com.walking.backend.domain.exception.InvalidFileException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Board;
import com.walking.backend.domain.model.Task;
import com.walking.backend.domain.model.TaskAttachment;
import com.walking.backend.props.AppProperties;
import com.walking.backend.repository.TaskAttachmentRepository;
import com.walking.backend.repository.TaskRepository;
import com.walking.backend.service.TaskAttachmentService;
import com.walking.backend.service.UserService;
import com.walking.backend.service.mapper.attachment.TaskAttachmentDownloadResponseMapper;
import com.walking.backend.service.mapper.attachment.TaskAttachmentResponseMapper;
import com.walking.backend.storage.service.FileStorageService;
import com.walking.backend.util.UserMapLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static com.walking.backend.domain.model.ActivityType.TASK_ATTACHMENT_ADDED;
import static com.walking.backend.domain.model.ActivityType.TASK_ATTACHMENT_DELETED;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaskAttachmentServiceImpl implements TaskAttachmentService {
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final TaskRepository taskRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final ActivityService activityService;
    private final TaskAttachmentResponseMapper taskAttachmentResponseMapper;
    private final TaskAttachmentDownloadResponseMapper taskAttachmentDownloadResponseMapper;
    private final AppProperties.Minio minioProperties;

    @Override
    @PreAuthorize("@resourceAccessService.canViewTask(#taskId, principal.id)")
    public List<TaskAttachmentResponse> getAttachments(Long taskId) {
        List<TaskAttachment> attachments = taskAttachmentRepository.findAllByTaskId(taskId);

        Map<Long, UserShortResponse> users = UserMapLoader
                .loadUserMap(attachments, TaskAttachment::getUploadedBy, userService);

        return attachments.stream()
                .map(attachment -> {
                    UserShortResponse uploader = attachment.getUploadedBy() != null
                            ? users.get(attachment.getUploadedBy().getId())
                            : null;

                    return taskAttachmentResponseMapper.toDto(attachment, uploader);
                })
                .toList();
    }

    @Override
    @PreAuthorize("@resourceAccessService.canViewTask(#taskId, principal.id)")
    public TaskAttachmentDownloadResponse getDownloadAttachment(Long taskId, Long attachmentId) {
        TaskAttachment attachment = taskAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ObjectNotFoundException("Attachment with id %d not found"
                        .formatted(attachmentId)));

        if (!attachment.getTask().getId().equals(taskId)) {
            throw new IllegalOperationException("Attachment %d does not belong to task %d".formatted(attachmentId, taskId));
        }

        String url = fileStorageService.generatePresignedUrl(attachment.getFilePath());

        return taskAttachmentDownloadResponseMapper.toDto(attachment, url);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canEditTask(#taskId, #userId)")
    public TaskAttachmentResponse uploadAttachment(Long taskId, Long userId, MultipartFile file) {
        validate(taskId, file);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id %d not found".formatted(taskId)));
        Board board = task.getSection().getBoard();

        String path = fileStorageService.uploadAttachment(taskId, file);

        TaskAttachment attachment = TaskAttachment.builder()
                .fileName(file.getOriginalFilename())
                .filePath(path)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .task(task)
                .uploadedBy(userService.getProxyUserById(userId))
                .build();

        TaskAttachment savedAttachment = taskAttachmentRepository.save(attachment);

        activityService.publish(board, TASK_ATTACHMENT_ADDED,
                "Added file %s to task %s".formatted(attachment.getFileName(), task.getTitle()));

        return taskAttachmentResponseMapper.toDto(savedAttachment, userService.getUserShortById(userId));
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canEditTask(#taskId, principal.id)")
    public void deleteAttachment(Long taskId, Long attachmentId) {
        TaskAttachment attachment = taskAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ObjectNotFoundException("Attachment with id %d not found"
                        .formatted(attachmentId)));

        Task task = attachment.getTask();
        Board board = task.getSection().getBoard();

        if (!task.getId().equals(taskId)) {
            throw new IllegalOperationException("Attachment %d does not belong to task %d".formatted(attachmentId, taskId));
        }

        fileStorageService.deleteAttachment(attachment.getFilePath());
        taskAttachmentRepository.delete(attachment);

        activityService.publish(board, TASK_ATTACHMENT_DELETED,
                "Deleted file %s from task %s".formatted(attachment.getFileName(), task.getTitle()));
    }

    private void validate(Long taskId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        if (!minioProperties.getAttachment().getAllowedContentTypes().contains(file.getContentType())) {
            throw new InvalidFileException("File type %s is not allowed".formatted(file.getContentType()));
        }

        long current = taskAttachmentRepository.countByTaskId(taskId);
        if (current >= minioProperties.getAttachment().getMaxPerTask()) {
            throw new AttachmentLimitExceededException("Task cannot have more than %d attachments"
                    .formatted(minioProperties.getAttachment().getMaxPerTask()));
        }
    }
}
