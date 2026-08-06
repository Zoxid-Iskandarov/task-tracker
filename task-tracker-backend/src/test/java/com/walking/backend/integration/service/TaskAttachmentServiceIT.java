package com.walking.backend.integration.service;

import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.attachment.TaskAttachmentDownloadResponse;
import com.walking.backend.domain.dto.attachment.TaskAttachmentResponse;
import com.walking.backend.domain.exception.AttachmentLimitExceededException;
import com.walking.backend.domain.exception.IllegalOperationException;
import com.walking.backend.domain.exception.InvalidFileException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.TaskAttachment;
import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.integration.annotation.WithMockUser;
import com.walking.backend.integration.util.MinioTestHelper;
import com.walking.backend.props.AppProperties;
import com.walking.backend.repository.TaskAttachmentRepository;
import com.walking.backend.service.TaskAttachmentService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static com.walking.backend.domain.model.ActivityType.TASK_ATTACHMENT_ADDED;
import static com.walking.backend.domain.model.ActivityType.TASK_ATTACHMENT_DELETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@WithMockUser
@RequiredArgsConstructor
public class TaskAttachmentServiceIT extends IntegrationTestBase {
    private final TaskAttachmentService taskAttachmentService;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final ActivityService activityService;

    private final MinioTestHelper minioTestHelper;
    private final AppProperties appProperties;

    @Test
    void getAttachments_whenTaskExistsAndUserHasAccess_shouldReturnAttachmentsList() {
        List<TaskAttachmentResponse> actual = taskAttachmentService.getAttachments(1L);

        assertThat(actual).isNotEmpty();
        assertThat(actual)
                .extracting(TaskAttachmentResponse::fileName)
                .contains("spec.pdf");
    }

    @Test
    void getAttachments_whenTaskHasNoAttachments_shouldReturnEmptyList() {
        List<TaskAttachmentResponse> actual = taskAttachmentService.getAttachments(3L);

        assertThat(actual).isEmpty();
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void getAttachments_whenUserHasNoAccess_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> taskAttachmentService.getAttachments(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getDownloadAttachment_whenAttachmentExists_shouldReturnDownloadResponse() {
        TaskAttachmentDownloadResponse response = taskAttachmentService.getDownloadAttachment(1L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.fileName()).isEqualTo("spec.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.url()).isNotBlank();
    }

    @Test
    void getDownloadAttachment_whenAttachmentDoesNotBelongToTask_shouldThrowIllegalOperationException() {
        assertThatThrownBy(() -> taskAttachmentService.getDownloadAttachment(2L, 1L))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Attachment 1 does not belong to task 2");
    }

    @Test
    void getDownloadAttachment_whenAttachmentNotFound_shouldThrowObjectNotFoundException() {
        assertThatThrownBy(() -> taskAttachmentService.getDownloadAttachment(1L, 99L))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Attachment with id 99 not found");
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void getDownloadAttachment_whenUserHasNoAccess_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> taskAttachmentService.getDownloadAttachment(1L, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void uploadAttachment_whenValidFileAndUserCanEditTask_shouldUploadAndSaveAttachment() {
        var file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "pdf content".getBytes());

        TaskAttachmentResponse response = taskAttachmentService.uploadAttachment(1L, 2L, file);

        assertThat(response).isNotNull();
        assertThat(response.fileName()).isEqualTo("document.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");

        Optional<TaskAttachment> saved = taskAttachmentRepository.findById(response.id());
        assertThat(saved).isPresent();
        assertThat(saved.get().getFileName()).isEqualTo("document.pdf");

        verify(activityService).publish(any(), eq(TASK_ATTACHMENT_ADDED), eq("Added file document.pdf to task Test Task With Two Assignees"));
    }

    @Test
    void uploadAttachment_whenFileIsEmpty_shouldThrowInvalidFileException() {
        var emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> taskAttachmentService.uploadAttachment(1L, 2L, emptyFile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("File is empty");
    }

    @Test
    void uploadAttachment_whenInvalidContentType_shouldThrowInvalidFileException() {
        var videoFile = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "video content".getBytes());

        assertThatThrownBy(() -> taskAttachmentService.uploadAttachment(1L, 2L, videoFile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("File type video/mp4 is not allowed");
    }

    @Test
    void uploadAttachment_whenAttachmentLimitExceeded_shouldThrowAttachmentLimitExceededException() {
        for (int i = 0; i < appProperties.getMinio().getAttachment().getMaxPerTask() - 1; i++) {
            var file = new MockMultipartFile(
                    "file", "file" + i + ".pdf", "application/pdf", "content".getBytes());
            taskAttachmentService.uploadAttachment(1L, 2L, file);
        }

        var oneMoreFile = new MockMultipartFile(
                "file", "one-more.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> taskAttachmentService.uploadAttachment(1L, 2L, oneMoreFile))
                .isInstanceOf(AttachmentLimitExceededException.class)
                .hasMessageContaining("Task cannot have more than");
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe") // john_doe is not a member of board 1, cannot edit task 1
    void uploadAttachment_whenUserCannotEditTask_shouldThrowAccessDeniedException() {
        var file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "pdf content".getBytes());

        assertThatThrownBy(() -> taskAttachmentService.uploadAttachment(1L, 1L, file))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteAttachment_whenValidAndUserCanEditTask_shouldDeleteAttachmentAndFileAndPublishActivity() {
        var file = new MockMultipartFile(
                "file", "to-delete.pdf", "application/pdf", "content".getBytes());
        TaskAttachmentResponse uploaded = taskAttachmentService.uploadAttachment(1L, 2L, file);

        TaskAttachment attachment = taskAttachmentRepository.findById(uploaded.id()).orElseThrow();
        String filePath = attachment.getFilePath();

        assertThat(attachmentExists(filePath)).isTrue();

        taskAttachmentService.deleteAttachment(1L, uploaded.id());

        assertThat(taskAttachmentRepository.findById(uploaded.id())).isEmpty();

        assertThat(attachmentExists(filePath)).isFalse();

        verify(activityService).publish(any(), eq(TASK_ATTACHMENT_DELETED),
                contains("Deleted file to-delete.pdf from task Test Task With Two Assignees"));
    }

    @Test
    void deleteAttachment_whenAttachmentDoesNotBelongToTask_shouldThrowIllegalOperationException() {
        assertThatThrownBy(() -> taskAttachmentService.deleteAttachment(2L, 1L))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Attachment 1 does not belong to task 2");
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe") // cannot edit task 1
    void deleteAttachment_whenUserCannotEditTask_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> taskAttachmentService.deleteAttachment(1L, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteAttachment_whenAttachmentNotFound_shouldThrowObjectNotFoundException() {
        assertThatThrownBy(() -> taskAttachmentService.deleteAttachment(1L, 999L))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("Attachment with id 999 not found");
    }

    @AfterEach
    void clearAttachmentBucket() {
        minioTestHelper.clearBucket(appProperties.getMinio().getBucketAttachment());
    }

    private boolean attachmentExists(String objectName) {
        return minioTestHelper.objectExists(appProperties.getMinio().getBucketAttachment(), objectName);
    }
}
