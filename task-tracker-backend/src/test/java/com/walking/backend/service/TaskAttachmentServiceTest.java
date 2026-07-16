package com.walking.backend.service;

import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.attachment.TaskAttachmentDownloadResponse;
import com.walking.backend.domain.dto.attachment.TaskAttachmentResponse;
import com.walking.backend.domain.dto.user.UserShortResponse;
import com.walking.backend.domain.exception.AttachmentLimitExceededException;
import com.walking.backend.domain.exception.IllegalOperationException;
import com.walking.backend.domain.exception.InvalidFileException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.*;
import com.walking.backend.props.AppProperties;
import com.walking.backend.repository.TaskAttachmentRepository;
import com.walking.backend.repository.TaskRepository;
import com.walking.backend.service.impl.TaskAttachmentServiceImpl;
import com.walking.backend.service.mapper.attachment.TaskAttachmentDownloadResponseMapper;
import com.walking.backend.service.mapper.attachment.TaskAttachmentResponseMapper;
import com.walking.backend.storage.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.walking.backend.domain.model.ActivityType.TASK_ATTACHMENT_ADDED;
import static com.walking.backend.domain.model.ActivityType.TASK_ATTACHMENT_DELETED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskAttachmentServiceTest {
    private static final Long ATTACHMENT_ID = 1L;
    private static final Long TASK_ID = 2L;
    private static final Long USER_ID = 3L;
    private static final String USER_USERNAME = "Dante";

    @Mock
    private TaskAttachmentRepository taskAttachmentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserService userService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ActivityService activityService;

    @Mock
    private TaskAttachmentResponseMapper taskAttachmentResponseMapper;

    @Mock
    private TaskAttachmentDownloadResponseMapper taskAttachmentDownloadResponseMapper;

    @Mock
    private AppProperties.Minio minioProperties;

    @InjectMocks
    private TaskAttachmentServiceImpl taskAttachmentService;

    @Test
    void getAttachments_attachmentHasAuthor_shouldReturnPageOfTaskAttachmentResponses() {
        var task = getTask();
        var user1 = buildUser(1L, "Dante");
        var user2 = buildUser(2L, "Anakin");
        var attachment1 = buildTaskAttachment(1L, user1, task);
        var attachment2 = buildTaskAttachment(2L, user2, task);

        var userShort1 = buildUserShortResponse(user1);
        var userShort2 = buildUserShortResponse(user2);

        var taskAttachmentResponse1 = buildTaskAttachmentResponse(attachment1, userShort1);
        var taskAttachmentResponse2 = buildTaskAttachmentResponse(attachment2, userShort2);

        doReturn(List.of(attachment1, attachment2)).when(taskAttachmentRepository).findAllByTaskId(task.getId());
        doReturn(List.of(userShort1, userShort2)).when(userService).getUserShortsByIds(Set.of(user1.getId(), user2.getId()));
        doReturn(taskAttachmentResponse1).when(taskAttachmentResponseMapper).toDto(attachment1, userShort1);
        doReturn(taskAttachmentResponse2).when(taskAttachmentResponseMapper).toDto(attachment2, userShort2);

        List<TaskAttachmentResponse> actual = taskAttachmentService.getAttachments(task.getId());

        assertFalse(actual.isEmpty());
        assertTrue(actual.contains(taskAttachmentResponse1));
        assertTrue(actual.contains(taskAttachmentResponse2));
        assertEquals(List.of(taskAttachmentResponse1, taskAttachmentResponse2), actual);

        verify(taskAttachmentRepository).findAllByTaskId(task.getId());
        verify(userService).getUserShortsByIds(Set.of(user1.getId(), user2.getId()));
        verify(taskAttachmentResponseMapper).toDto(attachment1, userShort1);
        verify(taskAttachmentResponseMapper).toDto(attachment2, userShort2);
    }

    @Test
    void getAttachments_attachmentHasNoAuthor_shouldReturnPageOfTaskAttachmentResponsesWithNullAuthorAndSkipUserServiceCall() {
        var task = getTask();
        var attachment1 = buildTaskAttachment(1L, null, task);
        var attachment2 = buildTaskAttachment(2L, null, task);

        var taskAttachmentResponse1 = buildTaskAttachmentResponse(attachment1, null);
        var taskAttachmentResponse2 = buildTaskAttachmentResponse(attachment2, null);

        doReturn(List.of(attachment1, attachment2)).when(taskAttachmentRepository).findAllByTaskId(task.getId());
        doReturn(taskAttachmentResponse1).when(taskAttachmentResponseMapper).toDto(attachment1, null);
        doReturn(taskAttachmentResponse2).when(taskAttachmentResponseMapper).toDto(attachment2, null);

        List<TaskAttachmentResponse> actual = taskAttachmentService.getAttachments(task.getId());

        assertFalse(actual.isEmpty());
        assertTrue(actual.contains(taskAttachmentResponse1));
        assertTrue(actual.contains(taskAttachmentResponse2));
        assertEquals(List.of(taskAttachmentResponse1, taskAttachmentResponse2), actual);

        verify(taskAttachmentRepository).findAllByTaskId(task.getId());
        verify(userService, never()).getUserShortsByIds(anySet());
        verify(taskAttachmentResponseMapper).toDto(attachment1, null);
        verify(taskAttachmentResponseMapper).toDto(attachment2, null);
    }

    @Test
    void getDownloadAttachment_whenAttachmentNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(taskAttachmentRepository).findById(ATTACHMENT_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskAttachmentService.getDownloadAttachment(TASK_ID, ATTACHMENT_ID));

        verify(taskAttachmentRepository).findById(ATTACHMENT_ID);
        verifyNoInteractions(fileStorageService, taskAttachmentDownloadResponseMapper);
    }

    @Test
    void getDownloadAttachment_whenAttachmentNotBelongToTask_shouldThrowIllegalOperationException() {
        var task = getTask();
        var user = buildUser(USER_ID, USER_USERNAME);
        var attachment = buildTaskAttachment(ATTACHMENT_ID, user, task);

        doReturn(Optional.of(attachment)).when(taskAttachmentRepository).findById(ATTACHMENT_ID);

        assertThrows(IllegalOperationException.class, () -> taskAttachmentService.getDownloadAttachment(10L, ATTACHMENT_ID));

        verify(taskAttachmentRepository).findById(ATTACHMENT_ID);
        verifyNoInteractions(fileStorageService, taskAttachmentDownloadResponseMapper);
    }

    @Test
    void getDownloadAttachment_whenValidRequestData_shouldReturnTaskAttachmentDownloadResponse() {
        var task = getTask();
        var user = buildUser(USER_ID, USER_USERNAME);
        var attachment = buildTaskAttachment(ATTACHMENT_ID, user, task);
        var expected = buildTaskAttachmentDownloadResponse(attachment);

        doReturn(Optional.of(attachment)).when(taskAttachmentRepository).findById(attachment.getId());
        doReturn(expected.url()).when(fileStorageService).generatePresignedUrl(attachment.getFilePath());
        doReturn(expected).when(taskAttachmentDownloadResponseMapper).toDto(attachment, expected.url());

        TaskAttachmentDownloadResponse actual = taskAttachmentService.getDownloadAttachment(task.getId(), attachment.getId());

        assertEquals(expected.url(), actual.url());
        assertEquals(expected, actual);

        verify(taskAttachmentRepository).findById(ATTACHMENT_ID);
        verify(fileStorageService).generatePresignedUrl(attachment.getFilePath());
        verify(taskAttachmentDownloadResponseMapper).toDto(attachment, expected.url());
    }

    @ParameterizedTest
    @MethodSource("provideInvalidFiles")
    void uploadAttachment_whenFileIsNullAndEmpty_shouldThrowInvalidFileException(MultipartFile file) {
        assertThrows(InvalidFileException.class, () -> taskAttachmentService.uploadAttachment(TASK_ID, USER_ID, file));

        verifyNoInteractions(taskRepository, fileStorageService, userService, taskAttachmentRepository,
                activityService, taskAttachmentResponseMapper);
    }

    @Test
    void uploadAttachment_whenUnsupportedFileType_shouldThrowInvalidFileException() {
        var file = mock(MultipartFile.class);
        var attachment = mock(AppProperties.Minio.Attachment.class);

        doReturn(false).when(file).isEmpty();
        doReturn("video/mp4").when(file).getContentType();
        doReturn(attachment).when(minioProperties).getAttachment();
        doReturn(Set.of(MediaType.APPLICATION_PDF_VALUE)).when(attachment).getAllowedContentTypes();

        assertThrows(InvalidFileException.class, () -> taskAttachmentService.uploadAttachment(TASK_ID, USER_ID, file));

        verifyNoInteractions(taskRepository, fileStorageService, userService, taskAttachmentRepository,
                activityService, taskAttachmentResponseMapper);
    }

    @Test
    void uploadAttachment_whenAttachmentLimitExceeded_shouldThrowAttachmentLimitExceededException() {
        var file = mock(MultipartFile.class);
        var attachmentProperties = mock(AppProperties.Minio.Attachment.class);

        doReturn(false).when(file).isEmpty();
        doReturn(MediaType.APPLICATION_PDF_VALUE).when(file).getContentType();
        doReturn(attachmentProperties).when(minioProperties).getAttachment();
        doReturn(Set.of(MediaType.APPLICATION_PDF_VALUE)).when(attachmentProperties).getAllowedContentTypes();
        doReturn(5L).when(taskAttachmentRepository).countByTaskId(TASK_ID);
        doReturn(5).when(attachmentProperties).getMaxPerTask();

        assertThrows(AttachmentLimitExceededException.class, () -> taskAttachmentService.uploadAttachment(TASK_ID, USER_ID, file));

        verify(taskAttachmentRepository).countByTaskId(TASK_ID);
        verify(taskAttachmentRepository, never()).save(any(TaskAttachment.class));
        verifyNoInteractions(taskRepository, fileStorageService, userService,
                activityService, taskAttachmentResponseMapper);
    }
    
    @Test
    void uploadAttachment_whenTaskNotFound_shouldThrowObjectNotFoundException() {
        var file = mock(MultipartFile.class);
        var attachmentProperties = mock(AppProperties.Minio.Attachment.class);

        doReturn(false).when(file).isEmpty();
        doReturn(MediaType.APPLICATION_PDF_VALUE).when(file).getContentType();
        doReturn(attachmentProperties).when(minioProperties).getAttachment();
        doReturn(Set.of(MediaType.APPLICATION_PDF_VALUE)).when(attachmentProperties).getAllowedContentTypes();
        doReturn(2L).when(taskAttachmentRepository).countByTaskId(TASK_ID);
        doReturn(5).when(attachmentProperties).getMaxPerTask();
        doReturn(Optional.empty()).when(taskRepository).findById(TASK_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskAttachmentService.uploadAttachment(TASK_ID, USER_ID, file));

        verify(taskAttachmentRepository).countByTaskId(TASK_ID);
        verify(taskRepository).findById(TASK_ID);
        verify(taskAttachmentRepository, never()).save(any(TaskAttachment.class));
        verifyNoInteractions(fileStorageService, userService, activityService, taskAttachmentResponseMapper);
    }

    @Test
    void uploadAttachment_whenValidRequestData_shouldUploadAttachmentAndReturnResponse() {
        var file = mock(MultipartFile.class);
        var attachmentProperties = mock(AppProperties.Minio.Attachment.class);
        var task = getTask();
        task.setTitle("Fix login bug");
        var board = task.getSection().getBoard();

        var user = buildUser(USER_ID, USER_USERNAME);
        var userShortResponse = buildUserShortResponse(user);
        var filePath = "attachments/2/uuid-file.pdf";

        doReturn(false).when(file).isEmpty();
        doReturn(MediaType.APPLICATION_PDF_VALUE).when(file).getContentType();
        doReturn("technical_spec.pdf").when(file).getOriginalFilename();
        doReturn(1024L).when(file).getSize();

        doReturn(attachmentProperties).when(minioProperties).getAttachment();
        doReturn(Set.of(MediaType.APPLICATION_PDF_VALUE)).when(attachmentProperties).getAllowedContentTypes();
        doReturn(2L).when(taskAttachmentRepository).countByTaskId(TASK_ID);
        doReturn(5).when(attachmentProperties).getMaxPerTask();

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(filePath).when(fileStorageService).uploadAttachment(TASK_ID, file);
        doReturn(user).when(userService).getProxyUserById(USER_ID);

        ArgumentCaptor<TaskAttachment> attachmentCaptor = ArgumentCaptor.forClass(TaskAttachment.class);
        doAnswer(invocation -> invocation.getArgument(0))
                .when(taskAttachmentRepository).save(any(TaskAttachment.class));

        doReturn(userShortResponse).when(userService).getUserShortById(USER_ID);

        var attachment = buildTaskAttachment(ATTACHMENT_ID, user, task);
        var expected = buildTaskAttachmentResponse(attachment, userShortResponse);

        doReturn(expected).when(taskAttachmentResponseMapper).toDto(any(TaskAttachment.class), eq(userShortResponse));

        TaskAttachmentResponse actual = taskAttachmentService.uploadAttachment(TASK_ID, USER_ID, file);

        assertEquals(expected, actual);

        verify(fileStorageService).uploadAttachment(TASK_ID, file);
        verify(taskAttachmentRepository).save(attachmentCaptor.capture());

        TaskAttachment savedAttachment = attachmentCaptor.getValue();
        assertEquals("technical_spec.pdf", savedAttachment.getFileName());
        assertEquals(filePath, savedAttachment.getFilePath());
        assertEquals(MediaType.APPLICATION_PDF_VALUE, savedAttachment.getContentType());
        assertEquals(1024L, savedAttachment.getFileSize());
        assertEquals(task, savedAttachment.getTask());
        assertEquals(user, savedAttachment.getUploadedBy());

        verify(activityService).publish(board, TASK_ATTACHMENT_ADDED,
                "Added file technical_spec.pdf to task Fix login bug");
        verify(userService).getUserShortById(USER_ID);
    }

    @Test
    void deleteAttachment_whenAttachmentNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(taskAttachmentRepository).findById(ATTACHMENT_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskAttachmentService.deleteAttachment(TASK_ID, ATTACHMENT_ID));

        verify(taskAttachmentRepository).findById(ATTACHMENT_ID);
        verify(taskAttachmentRepository, never()).delete(any(TaskAttachment.class));
        verifyNoInteractions(fileStorageService, activityService);
    }

    @Test
    void deleteAttachment_whenAttachmentNotBelongedToTask_shouldThrowIllegalOperationException() {
        var task = getTask();
        var user = buildUser(USER_ID, USER_USERNAME);
        var attachment = buildTaskAttachment(ATTACHMENT_ID, user, task);

        doReturn(Optional.of(attachment)).when(taskAttachmentRepository).findById(attachment.getId());

        assertThrows(IllegalOperationException.class, () -> taskAttachmentService.deleteAttachment(10L, attachment.getId()));

        verify(taskAttachmentRepository).findById(attachment.getId());
        verify(taskAttachmentRepository, never()).delete(any(TaskAttachment.class));
        verifyNoInteractions(fileStorageService, activityService);
    }

    @Test
    void deleteAttachment_whenValidRequestData_shouldDeleteAttachment() {
        var task = getTask();
        var board = task.getSection().getBoard();
        var user = buildUser(USER_ID, USER_USERNAME);
        var attachment = buildTaskAttachment(ATTACHMENT_ID, user, task);

        doReturn(Optional.of(attachment)).when(taskAttachmentRepository).findById(attachment.getId());

        taskAttachmentService.deleteAttachment(task.getId(), attachment.getId());

        verify(taskAttachmentRepository).findById(attachment.getId());
        verify(fileStorageService).deleteAttachment(attachment.getFilePath());
        verify(taskAttachmentRepository).delete(attachment);
        verify(activityService).publish(board, TASK_ATTACHMENT_DELETED,
                "Deleted file %s from task %s".formatted(attachment.getFileName(), task.getTitle()));
    }

    private static Stream<MultipartFile> provideInvalidFiles() {
        MultipartFile emptyFile = mock(MultipartFile.class);
        doReturn(true).when(emptyFile).isEmpty();

        return Stream.of(null, emptyFile);
    }

    private TaskAttachment buildTaskAttachment(Long attachmentId, User user, Task task) {
        TaskAttachment attachment = new TaskAttachment();
        attachment.setId(attachmentId);
        attachment.setFileName("technical_specification_v2.pdf");
        attachment.setFilePath("path/to/file");
        attachment.setFileSize(1024L);
        attachment.setContentType(MediaType.APPLICATION_PDF_VALUE);
        attachment.setTask(task);
        attachment.setUploadedBy(user);

        return attachment;
    }

    private User buildUser(Long userId, String username) {
        User user = new User();
        user.setId(userId);
        user.setUsername(username);

        return user;
    }

    private Task getTask() {
        Board board = new Board();
        board.setId(1L);

        Section section = new Section();
        section.setId(1L);
        section.setBoard(board);

        Task task = new Task();
        task.setId(TASK_ID);
        task.setSection(section);

        return task;
    }

    private UserShortResponse buildUserShortResponse(User user) {
        return new UserShortResponse(user.getId(), user.getUsername(), "", "");
    }

    private TaskAttachmentResponse buildTaskAttachmentResponse(
            TaskAttachment attachment,
            UserShortResponse userShortResponse) {
        return new TaskAttachmentResponse(attachment.getId(), attachment.getFileName(), attachment.getContentType(),
                attachment.getFileSize(), userShortResponse, LocalDateTime.now());
    }

    private TaskAttachmentDownloadResponse buildTaskAttachmentDownloadResponse(TaskAttachment attachment) {
        return new TaskAttachmentDownloadResponse(
                "temp-url/attachments/technical_spec_v2.pdf", attachment.getFileName(), attachment.getContentType());
    }
}
