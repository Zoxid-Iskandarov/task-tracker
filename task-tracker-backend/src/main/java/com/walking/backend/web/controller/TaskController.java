package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.attachment.TaskAttachmentDownloadResponse;
import com.walking.backend.domain.dto.attachment.TaskAttachmentResponse;
import com.walking.backend.domain.dto.task.*;
import com.walking.backend.security.principal.CustomUserDetails;
import com.walking.backend.service.TaskAttachmentService;
import com.walking.backend.service.TaskService;
import com.walking.backend.web.openapi.TaskApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController implements TaskApi {
    private final TaskService taskService;
    private final TaskAttachmentService taskAttachmentService;

    @GetMapping("/{taskId}")
    public TaskFullResponse getTaskById(@PathVariable Long taskId) {
        return taskService.getTaskById(taskId);
    }

    @PatchMapping("/{taskId}/toggle")
    public TaskPreviewResponse toggleCompleted(@PathVariable Long taskId) {
        return taskService.toggleCompleted(taskId);
    }

    @PatchMapping("/{taskId}/move")
    public TaskPreviewResponse moveTask(
            @PathVariable Long taskId,
            @RequestBody @Validated MoveTaskRequest moveTaskRequest) {
        return taskService.moveTask(taskId, moveTaskRequest);
    }

    @PostMapping("/{taskId}/labels/{labelId}")
    public TaskPreviewResponse addLabelToTask(@PathVariable Long taskId, @PathVariable Long labelId) {
        return taskService.addLabelToTask(taskId, labelId);
    }

    @DeleteMapping("/{taskId}/labels/{labelId}")
    public TaskPreviewResponse deleteLabelFromTask(@PathVariable Long taskId, @PathVariable Long labelId) {
        return taskService.deleteLabelFromTask(taskId, labelId);
    }

    @PostMapping
    public ResponseEntity<TaskFullResponse> createTask(@RequestBody @Validated CreateTaskRequest createTaskRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(createTaskRequest));
    }

    @PutMapping("/{taskId}")
    public TaskFullResponse updateTask(
            @RequestBody @Validated UpdateTaskRequest updateTaskRequest,
            @PathVariable Long taskId) {
        return taskService.updateTask(updateTaskRequest, taskId);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/attachments")
    public List<TaskAttachmentResponse> getAttachments(@PathVariable Long taskId) {
        return taskAttachmentService.getAttachments(taskId);
    }

    @GetMapping("/{taskId}/attachments/{attachmentId}")
    public TaskAttachmentDownloadResponse getDownloadAttachment(@PathVariable Long taskId, @PathVariable Long attachmentId) {
        return taskAttachmentService.getDownloadAttachment(taskId, attachmentId);
    }

    @PostMapping(value = "/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TaskAttachmentResponse uploadAttachment(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskAttachmentService.uploadAttachment(taskId, userDetails.id(), file);
    }

    @DeleteMapping("/{taskId}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long taskId, @PathVariable Long attachmentId) {
        taskAttachmentService.deleteAttachment(taskId, attachmentId);

        return ResponseEntity.noContent().build();
    }
}
