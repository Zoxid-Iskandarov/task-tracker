package com.walking.backend.service;

import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.task.*;
import com.walking.backend.domain.dto.user.UserShortResponse;
import com.walking.backend.domain.exception.*;
import com.walking.backend.domain.model.*;
import com.walking.backend.props.AppProperties;
import com.walking.backend.repository.TaskRepository;
import com.walking.backend.service.impl.TaskServiceImpl;
import com.walking.backend.service.mapper.task.CreateTaskRequestMapper;
import com.walking.backend.service.mapper.task.TaskFullResponseMapper;
import com.walking.backend.service.mapper.task.TaskPreviewResponseMapper;
import com.walking.backend.storage.service.ResourceCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static com.walking.backend.domain.model.ActivityType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {
    private static final Long TASK_ID = 1L;
    private static final String TASK_TITLE = "Fix bug";
    private static final Long BOARD_ID = 2L;
    private static final Long SECTION_ID = 3L;
    private static final Long USER_ID = 4L;
    private static final String USER_USERNAME = "Dante";
    private static final Long LABEL_ID = 5L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private SectionService sectionService;

    @Mock
    private UserService userService;

    @Mock
    private LabelService labelService;

    @Mock
    private ActivityService activityService;

    @Mock
    private ResourceCleanupService resourceCleanupService;

    @Mock
    private CreateTaskRequestMapper createTaskRequestMapper;

    @Mock
    private TaskFullResponseMapper taskFullResponseMapper;

    @Mock
    private TaskPreviewResponseMapper taskPreviewResponseMapper;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void getTasks_whenTasksExist_shouldReturnPageOfTaskPreviewResponses() {
        var pageable = PageRequest.of(0, 10);
        var task1 = buildTask(1L, "Fix bug");
        var task2 = buildTask(2L, "Write docs");
        var tasksPage = new PageImpl<>(List.of(task1, task2), pageable, 2L);

        var user1 = buildUser(1L, "Dante");
        var assignee1 = buildUserShortResponse(user1);
        var assigneesByTaskId = Map.of(1L, List.of(assignee1), 2L, List.<UserShortResponse>of());

        var taskPreviewResponse1 = buildTaskPreviewResponse(task1, List.of(assignee1));
        var taskPreviewResponse2 = buildTaskPreviewResponse(task2, List.of());

        doReturn(tasksPage).when(taskRepository).findAll(any(Specification.class), eq(pageable));
        doReturn(assigneesByTaskId).when(userService).getAssigneeByTaskIds(Set.of(task1.getId(), task2.getId()));
        doReturn(taskPreviewResponse1).when(taskPreviewResponseMapper).toDto(task1, List.of(assignee1));
        doReturn(taskPreviewResponse2).when(taskPreviewResponseMapper).toDto(task2, List.of());

        Page<TaskPreviewResponse> actual = taskService.getTasks(SECTION_ID, pageable);

        assertTrue(actual.hasContent());
        assertEquals(List.of(taskPreviewResponse1, taskPreviewResponse2), actual.getContent());

        verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        verify(userService).getAssigneeByTaskIds(Set.of(task1.getId(), task2.getId()));
        verify(taskPreviewResponseMapper).toDto(task1, List.of(assignee1));
        verify(taskPreviewResponseMapper).toDto(task2, List.of());
    }

    @Test
    void getTasks_whenNoTasks_shouldReturnEmptyPageAndSkipAssigneeLookup() {
        var pageable = PageRequest.of(0, 10);
        var tasksPage = new PageImpl<>(List.of(), pageable, 0L);

        doReturn(tasksPage).when(taskRepository).findAll(any(Specification.class), eq(pageable));

        Page<TaskPreviewResponse> actual = taskService.getTasks(SECTION_ID, pageable);

        assertTrue(actual.isEmpty());

        verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoInteractions(userService, taskPreviewResponseMapper);
    }

    @Test
    void searchTasks_whenTasksFound_shouldReturnPageOfTaskPreviewResponses() {
        var pageable = PageRequest.of(0, 10);
        var taskFilter = buildEmptyTaskFilter();
        var task1 = buildTask(1L, "Fix bug");
        var task2 = buildTask(2L, "Write docs");
        var tasksPage = new PageImpl<>(List.of(task1, task2), pageable, 2L);

        var user1 = buildUser(1L, "Dante");
        var assignee1 = buildUserShortResponse(user1);
        var assigneesByTaskId = Map.of(1L, List.of(assignee1), 2L, List.<UserShortResponse>of());

        var taskPreviewResponse1 = buildTaskPreviewResponse(task1, List.of(assignee1));
        var taskPreviewResponse2 = buildTaskPreviewResponse(task2, List.of());

        doReturn(tasksPage).when(taskRepository).findAll(any(Specification.class), eq(pageable));
        doReturn(assigneesByTaskId).when(userService).getAssigneeByTaskIds(Set.of(task1.getId(), task2.getId()));
        doReturn(taskPreviewResponse1).when(taskPreviewResponseMapper).toDto(task1, List.of(assignee1));
        doReturn(taskPreviewResponse2).when(taskPreviewResponseMapper).toDto(task2, List.of());

        Page<TaskPreviewResponse> actual = taskService.searchTasks(BOARD_ID, taskFilter, pageable);

        assertTrue(actual.hasContent());
        assertEquals(List.of(taskPreviewResponse1, taskPreviewResponse2), actual.getContent());

        verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        verify(userService).getAssigneeByTaskIds(Set.of(task1.getId(), task2.getId()));
        verify(taskPreviewResponseMapper).toDto(task1, List.of(assignee1));
        verify(taskPreviewResponseMapper).toDto(task2, List.of());
    }

    @Test
    void searchTasks_whenNoTaskMatch_shouldReturnEmptyPageAndSkipAssigneeLookup() {
        var pageable = PageRequest.of(0, 10);
        var taskFilter = buildEmptyTaskFilter();
        var tasksPage = new PageImpl<>(List.of(), pageable, 0L);

        doReturn(tasksPage).when(taskRepository).findAll(any(Specification.class), eq(pageable));

        Page<TaskPreviewResponse> actual = taskService.searchTasks(BOARD_ID, taskFilter, pageable);

        assertTrue(actual.isEmpty());

        verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoInteractions(userService, taskPreviewResponseMapper);
    }

    @Test
    void getTaskById_whenTaskNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(taskRepository).findByIdWithLabels(TASK_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskService.getTaskById(TASK_ID));

        verify(taskRepository).findByIdWithLabels(TASK_ID);
        verifyNoInteractions(userService, taskFullResponseMapper);
    }

    @Test
    void getTaskById_whenTaskHasAssignees_shouldReturnTaskFullResponse() {
        var task = buildTask(TASK_ID, TASK_TITLE);
        var user = buildUser(USER_ID, USER_USERNAME);
        task.setAssignees(Set.of(user));

        var assignee = buildUserShortResponse(user);
        var taskFullResponse = buildTaskFullResponse(task, List.of(assignee));

        doReturn(Optional.of(task)).when(taskRepository).findByIdWithLabels(task.getId());
        doReturn(List.of(assignee)).when(userService).getUserShortsByIds(Set.of(user.getId()));
        doReturn(taskFullResponse).when(taskFullResponseMapper).toDto(task, List.of(assignee));

        TaskFullResponse actual = taskService.getTaskById(task.getId());

        assertEquals(taskFullResponse, actual);

        verify(taskRepository).findByIdWithLabels(TASK_ID);
        verify(userService).getUserShortsByIds(Set.of(user.getId()));
        verify(taskFullResponseMapper).toDto(task, List.of(assignee));
    }

    @Test
    void getTaskById_whenTaskNoHasAssignees_shouldReturnTaskFullResponseAndSkipAssigneeLookup() {
        var task = buildTask(TASK_ID, TASK_TITLE);
        var taskFullResponse = buildTaskFullResponse(task, List.of());

        doReturn(Optional.of(task)).when(taskRepository).findByIdWithLabels(task.getId());
        doReturn(taskFullResponse).when(taskFullResponseMapper).toDto(task, List.of());

        TaskFullResponse actual = taskService.getTaskById(task.getId());

        assertEquals(taskFullResponse, actual);

        verify(taskRepository).findByIdWithLabels(TASK_ID);
        verify(userService, never()).getUserShortsByIds(Set.of());
        verify(taskFullResponseMapper).toDto(task, List.of());
    }

    @Test
    void createTask_whenInvalidAssigneeId_shouldThrowInvalidTaskAssigneeException() {
        var request = buildCreateTaskRequest(Set.of(USER_ID));
        var task = buildTask(TASK_ID, TASK_TITLE);
        var section = task.getSection();
        var user = buildUser(10L, "SomeUsername");

        doReturn(task).when(createTaskRequestMapper).toEntity(request);
        doReturn(section).when(sectionService).getProxySectionById(request.sectionId());
        doReturn(Set.of(user)).when(userService).getBoardMembersForTask(request.sectionId(), request.assigneeIds());

        assertThrows(InvalidTaskAssigneeException.class, () -> taskService.createTask(request));

        verify(createTaskRequestMapper).toEntity(request);
        verify(sectionService).getProxySectionById(request.sectionId());
        verify(userService).getBoardMembersForTask(request.sectionId(), request.assigneeIds());
        verifyNoInteractions(appProperties, taskRepository, taskFullResponseMapper);
    }

    @Test
    void createTask_whenNoAssigneeIds_shouldCreateTaskWithoutAssigneesAndReturnTaskFullResponse() {
        var request = buildCreateTaskRequest(null);
        var task = buildTask(TASK_ID, TASK_TITLE);
        var section = task.getSection();
        var taskProperties = mock(AppProperties.Task.class);
        var taskFullResponse = buildTaskFullResponse(task, List.of());

        doReturn(task).when(createTaskRequestMapper).toEntity(request);
        doReturn(section).when(sectionService).getProxySectionById(request.sectionId());
        doReturn(taskProperties).when(appProperties).getTask();
        doReturn(15.0).when(taskProperties).getPositionStep();
        doReturn(null).when(taskRepository).findMaxPositionBySectionId(request.sectionId());
        doReturn(task).when(taskRepository).save(task);
        doReturn(taskFullResponse).when(taskFullResponseMapper).toDto(task, List.of());

        TaskFullResponse actual = taskService.createTask(request);

        assertEquals(taskFullResponse, actual);

        assertFalse(task.getIsCompleted());
        assertEquals(section, task.getSection());
        assertTrue(task.getAssignees().isEmpty());
        assertEquals(15.0, task.getPosition());

        verify(createTaskRequestMapper).toEntity(request);
        verify(sectionService).getProxySectionById(request.sectionId());
        verify(taskRepository).findMaxPositionBySectionId(request.sectionId());
        verify(taskRepository).save(task);
        verify(taskFullResponseMapper).toDto(task, List.of());
        verifyNoInteractions(userService);
    }

    @Test
    void createTask_whenHasAssigneeIds_shouldCreateTaskWithAssigneesAndReturnTaskFullResponse() {
        var request = buildCreateTaskRequest(Set.of(USER_ID));
        var user = buildUser(USER_ID, USER_USERNAME);
        var userShortResponse = buildUserShortResponse(user);
        var task = buildTask(TASK_ID, TASK_TITLE);
        var section = task.getSection();
        var taskProperties = mock(AppProperties.Task.class);
        var taskFullResponse = buildTaskFullResponse(task, List.of(userShortResponse));

        doReturn(task).when(createTaskRequestMapper).toEntity(request);
        doReturn(section).when(sectionService).getProxySectionById(request.sectionId());
        doReturn(Set.of(user)).when(userService).getBoardMembersForTask(request.sectionId(), request.assigneeIds());
        doReturn(taskProperties).when(appProperties).getTask();
        doReturn(15.0).when(taskProperties).getPositionStep();
        doReturn(null).when(taskRepository).findMaxPositionBySectionId(request.sectionId());
        doReturn(task).when(taskRepository).save(task);
        doReturn(List.of(userShortResponse)).when(userService).getUserShortsByIds(request.assigneeIds());
        doReturn(taskFullResponse).when(taskFullResponseMapper).toDto(task, List.of(userShortResponse));

        TaskFullResponse actual = taskService.createTask(request);

        assertEquals(taskFullResponse, actual);

        assertFalse(task.getIsCompleted());
        assertEquals(section, task.getSection());
        assertFalse(task.getAssignees().isEmpty());
        assertEquals(15.0, task.getPosition());

        verify(createTaskRequestMapper).toEntity(request);
        verify(sectionService).getProxySectionById(request.sectionId());
        verify(userService).getBoardMembersForTask(request.sectionId(), request.assigneeIds());
        verify(taskRepository).findMaxPositionBySectionId(request.sectionId());
        verify(taskRepository).save(task);
        verify(userService).getUserShortsByIds(request.assigneeIds());
        verify(taskFullResponseMapper).toDto(task, List.of(userShortResponse));
    }

    @Test
    void createTask_whenSectionHasExistingTasks_shouldCalculatePositionFromMax() {
        var request = buildCreateTaskRequest(null);
        var task = buildTask(TASK_ID, TASK_TITLE);
        var section = task.getSection();
        var taskProperties = mock(AppProperties.Task.class);
        var taskFullResponse = buildTaskFullResponse(task, List.of());

        doReturn(task).when(createTaskRequestMapper).toEntity(request);
        doReturn(section).when(sectionService).getProxySectionById(request.sectionId());
        doReturn(taskProperties).when(appProperties).getTask();
        doReturn(15.0).when(taskProperties).getPositionStep();
        doReturn(100.0).when(taskRepository).findMaxPositionBySectionId(request.sectionId());
        doReturn(task).when(taskRepository).save(task);
        doReturn(taskFullResponse).when(taskFullResponseMapper).toDto(task, List.of());

        TaskFullResponse actual = taskService.createTask(request);

        assertEquals(taskFullResponse, actual);
        assertEquals(115.0, task.getPosition());

        verify(createTaskRequestMapper).toEntity(request);
        verify(sectionService).getProxySectionById(request.sectionId());
        verify(taskRepository).findMaxPositionBySectionId(request.sectionId());
        verify(taskRepository).save(task);
        verify(taskFullResponseMapper).toDto(task, List.of());
        verifyNoInteractions(userService);
    }

    @Test
    void updateTask_whenTaskNotFound_shouldThrowObjectNotFoundException() {
        var updateTaskRequest = new UpdateTaskRequest(TASK_TITLE, "Description", null, null);

        doReturn(Optional.empty()).when(taskRepository).findById(TASK_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskService.updateTask(updateTaskRequest, TASK_ID));

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository, never()).save(any(Task.class));
        verifyNoInteractions(userService, activityService, taskFullResponseMapper);
    }

    @Test
    void updateTask_whenInvalidAssigneeId_shouldThrowInvalidTaskAssigneeException() {
        var request = new UpdateTaskRequest(TASK_TITLE, "Description", null, Set.of(USER_ID));
        var task = buildTask(TASK_ID, TASK_TITLE);
        var user = buildUser(10L, "SomeUsername");

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(Set.of(user)).when(userService).getBoardMembersForTask(task.getSection().getId(), request.assigneeIds());

        assertThrows(InvalidTaskAssigneeException.class, () -> taskService.updateTask(request, TASK_ID));

        verify(taskRepository).findById(TASK_ID);
        verify(userService).getBoardMembersForTask(task.getSection().getId(), request.assigneeIds());
        verify(taskRepository, never()).save(any(Task.class));
        verifyNoInteractions(activityService, taskFullResponseMapper);
    }

    @Test
    void updateTask_whenTitleChangedAndHasAssignees_shouldUpdateTaskAndPublishRenamedActivity() {
        var updateTaskRequest = new UpdateTaskRequest("New Title", "Description", null, Set.of(USER_ID));
        var task = buildTask(TASK_ID, "Old Title");
        var board = task.getSection().getBoard();
        var user = buildUser(USER_ID, USER_USERNAME);
        var userShortResponse = buildUserShortResponse(user);
        var taskFullResponse = buildTaskFullResponse(task, List.of(userShortResponse));

        doReturn(Optional.of(task)).when(taskRepository).findById(task.getId());
        doReturn(Set.of(user)).when(userService)
                .getBoardMembersForTask(task.getSection().getId(), updateTaskRequest.assigneeIds());
        doReturn(task).when(taskRepository).save(task);
        doReturn(List.of(userShortResponse)).when(userService).getUserShortsByIds(Set.of(USER_ID));
        doReturn(taskFullResponse).when(taskFullResponseMapper).toDto(task, List.of(userShortResponse));

        TaskFullResponse actual = taskService.updateTask(updateTaskRequest, task.getId());

        assertEquals(taskFullResponse, actual);
        assertEquals("New Title", task.getTitle());
        assertEquals("Description", task.getDescription());

        verify(taskRepository).findById(task.getId());
        verify(userService).getBoardMembersForTask(task.getSection().getId(), updateTaskRequest.assigneeIds());
        verify(taskRepository).save(task);
        verify(activityService).publish(board, TASK_UPDATED, "Renamed task from Old Title to New Title");
        verify(taskFullResponseMapper).toDto(task, List.of(userShortResponse));
    }

    @Test
    void updateTask_whenTitleUnchangedAndNoAssignees_shouldUpdateTaskAndPublishUpdatedActivity() {
        var sameTitle = "Same Title";
        var updateTaskRequest = new UpdateTaskRequest(sameTitle, "New Description", null, null);
        var task = buildTask(TASK_ID, sameTitle);
        var board = task.getSection().getBoard();
        var taskFullResponse = buildTaskFullResponse(task, List.of());

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(task).when(taskRepository).save(task);
        doReturn(taskFullResponse).when(taskFullResponseMapper).toDto(task, List.of());

        TaskFullResponse actual = taskService.updateTask(updateTaskRequest, TASK_ID);

        assertEquals(taskFullResponse, actual);
        assertTrue(task.getAssignees().isEmpty());

        verify(taskRepository).findById(task.getId());
        verify(taskRepository).save(task);
        verify(activityService).publish(board, TASK_UPDATED, "Updated task %s".formatted(sameTitle));
        verify(taskFullResponseMapper).toDto(task, List.of());
        verifyNoInteractions(userService);
    }

    @Test
    void deleteTask_whenTaskNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(taskRepository).findByIdWithAttachments(TASK_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskService.deleteTask(TASK_ID));

        verify(taskRepository).findByIdWithAttachments(TASK_ID);
        verify(taskRepository, never()).delete(any(Task.class));
        verifyNoInteractions(activityService, resourceCleanupService);
    }

    @Test
    void deleteTask_whenValidRequestData_shouldDeleteTask() {
        var task = buildTask(TASK_ID, TASK_TITLE);
        var attachments = List.of(buildAttachment(task, "path/to/file1"), buildAttachment(task, "path/to/file2"));
        task.setAttachments(attachments);

        var filePaths = List.of("path/to/file1", "path/to/file2");

        doReturn(Optional.of(task)).when(taskRepository).findByIdWithAttachments(TASK_ID);

        taskService.deleteTask(task.getId());

        verify(taskRepository).findByIdWithAttachments(task.getId());
        verify(taskRepository).delete(task);
        verify(activityService).publish(task.getSection().getBoard(), TASK_DELETED, "Deleted task %s".formatted(task.getTitle()));
        verify(resourceCleanupService).cleanupFiles(filePaths);
    }

    @Test
    void toggleCompleted_whenTaskNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(taskRepository).findById(TASK_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskService.toggleCompleted(TASK_ID));

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository, never()).save(any(Task.class));
        verifyNoInteractions(activityService, taskPreviewResponseMapper);
    }

    @Test
    void toggleCompleted_whenTaskWasNotCompleted_shouldMarkAsCompletedAndPublishCompletedActivity() {
        var task = buildTask(TASK_ID, TASK_TITLE);
        task.setIsCompleted(false);
        var board = task.getSection().getBoard();
        var taskPreviewResponse = buildTaskPreviewResponse(task, List.of());

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(task).when(taskRepository).save(task);
        doReturn(taskPreviewResponse).when(taskPreviewResponseMapper).toDto(task, List.of());

        TaskPreviewResponse actual = taskService.toggleCompleted(TASK_ID);

        assertEquals(taskPreviewResponse, actual);
        assertTrue(task.getIsCompleted());

        verify(taskRepository).save(task);
        verify(activityService).publish(board, TASK_COMPLETED, "Completed task %s".formatted(TASK_TITLE));
        verify(taskPreviewResponseMapper).toDto(task, List.of());
    }

    @Test
    void toggleCompleted_whenTaskWasCompleted_shouldMarkAsReopenedAndPublishReopenedActivity() {
        var task = buildTask(TASK_ID, TASK_TITLE);
        task.setIsCompleted(true);
        var board = task.getSection().getBoard();
        var taskPreviewResponse = buildTaskPreviewResponse(task, List.of());

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(task).when(taskRepository).save(task);
        doReturn(taskPreviewResponse).when(taskPreviewResponseMapper).toDto(task, List.of());

        TaskPreviewResponse actual = taskService.toggleCompleted(TASK_ID);

        assertEquals(taskPreviewResponse, actual);
        assertFalse(task.getIsCompleted());

        verify(taskRepository).save(task);
        verify(activityService).publish(board, TASK_REOPENED, "Reopened task %s".formatted(TASK_TITLE));
        verify(taskPreviewResponseMapper).toDto(task, List.of());
    }

    @Test
    void moveTask_whenTaskNotFound_shouldThrowObjectNotFoundException() {
        var moveTaskRequest = new MoveTaskRequest(SECTION_ID, 10L, 11L);

        doReturn(Optional.empty()).when(taskRepository).findById(TASK_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskService.moveTask(TASK_ID, moveTaskRequest));

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository, never()).findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository, never()).findByIdAndSectionId(moveTaskRequest.nextTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository, never()).findById(moveTaskRequest.prevTaskId());
        verify(taskRepository, never()).findById(moveTaskRequest.nextTaskId());
        verify(taskRepository, never()).save(any(Task.class));
        verifyNoInteractions(sectionService, activityService, taskPreviewResponseMapper);
    }

    @Test
    void moveTask_whenTaskByPrevTaskIdNotFound_shouldThrowTaskMoveException() {
        var moveTaskRequest = new MoveTaskRequest(SECTION_ID, 10L, 11L);
        var task = buildTask(TASK_ID, TASK_TITLE);

        doReturn(Optional.of(task)).when(taskRepository).findById(task.getId());
        doReturn(Optional.empty()).when(taskRepository)
                .findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());

        assertThrows(TaskMoveException.class, () -> taskService.moveTask(TASK_ID, moveTaskRequest));

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository).findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository, never()).findByIdAndSectionId(moveTaskRequest.nextTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository, never()).findById(moveTaskRequest.prevTaskId());
        verify(taskRepository, never()).findById(moveTaskRequest.nextTaskId());
        verify(taskRepository, never()).save(any(Task.class));
        verifyNoInteractions(sectionService, activityService, taskPreviewResponseMapper);
    }

    @Test
    void moveTask_whenTaskByNextTaskIdNotFound_shouldThrowTaskMoveException() {
        var moveTaskRequest = new MoveTaskRequest(SECTION_ID, 10L, 11L);
        var task = buildTask(TASK_ID, TASK_TITLE);
        var prevTask = buildTask(moveTaskRequest.prevTaskId(), "Prev task");

        doReturn(Optional.of(task)).when(taskRepository).findById(task.getId());
        doReturn(Optional.of(prevTask)).when(taskRepository)
                .findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());
        doReturn(Optional.empty()).when(taskRepository)
                .findByIdAndSectionId(moveTaskRequest.nextTaskId(), moveTaskRequest.sectionId());

        assertThrows(TaskMoveException.class, () -> taskService.moveTask(TASK_ID, moveTaskRequest));

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository).findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository).findByIdAndSectionId(moveTaskRequest.nextTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository, never()).findById(moveTaskRequest.prevTaskId());
        verify(taskRepository, never()).findById(moveTaskRequest.nextTaskId());
        verify(taskRepository, never()).save(any(Task.class));
        verifyNoInteractions(sectionService, activityService, taskPreviewResponseMapper);
    }

    @Test
    void moveTask_whenPrevTaskIdAndNextTaskIdTheSame_shouldThrowTaskMoveException() {
        var taskId = 10L;
        var moveTaskRequest = new MoveTaskRequest(SECTION_ID, taskId, taskId);
        var task = buildTask(TASK_ID, TASK_TITLE);
        var prevTask = buildTask(moveTaskRequest.prevTaskId(), "Some task");

        doReturn(Optional.of(task)).when(taskRepository).findById(task.getId());
        doReturn(Optional.of(prevTask)).when(taskRepository).findByIdAndSectionId(taskId, moveTaskRequest.sectionId());

        assertThrows(TaskMoveException.class, () -> taskService.moveTask(TASK_ID, moveTaskRequest));

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository, times(2)).findByIdAndSectionId(taskId, moveTaskRequest.sectionId());
        verify(taskRepository, never()).findById(moveTaskRequest.prevTaskId());
        verify(taskRepository, never()).findById(moveTaskRequest.nextTaskId());
        verify(taskRepository, never()).save(any(Task.class));
        verifyNoInteractions(sectionService, activityService, taskPreviewResponseMapper);
    }

    @Test
    void moveTask_whenTaskIdAndPrevTaskIdTheSame_shouldThrowTaskMoveException() {
        var moveTaskRequest = new MoveTaskRequest(SECTION_ID, TASK_ID, 10L);
        var task = buildTask(TASK_ID, TASK_TITLE);
        var nextTask = buildTask(moveTaskRequest.nextTaskId(), "Next task");

        doReturn(Optional.of(task)).when(taskRepository).findById(task.getId());
        doReturn(Optional.of(task)).when(taskRepository)
                .findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());
        doReturn(Optional.of(nextTask)).when(taskRepository)
                .findByIdAndSectionId(moveTaskRequest.nextTaskId(), moveTaskRequest.sectionId());

        assertThrows(TaskMoveException.class, () -> taskService.moveTask(TASK_ID, moveTaskRequest));

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository).findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository).findByIdAndSectionId(moveTaskRequest.nextTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository, never()).findById(moveTaskRequest.nextTaskId());
        verify(taskRepository, never()).save(any(Task.class));
        verifyNoInteractions(sectionService, activityService, taskPreviewResponseMapper);
    }

    @Test
    void moveTask_whenTaskIdAndNextTaskIdTheSame_shouldThrowTaskMoveException() {
        var moveTaskRequest = new MoveTaskRequest(SECTION_ID, 10L, TASK_ID);
        var task = buildTask(TASK_ID, TASK_TITLE);
        var prevTask = buildTask(moveTaskRequest.prevTaskId(), "Prev task");

        doReturn(Optional.of(task)).when(taskRepository).findById(task.getId());
        doReturn(Optional.of(prevTask)).when(taskRepository)
                .findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());
        doReturn(Optional.of(task)).when(taskRepository)
                .findByIdAndSectionId(moveTaskRequest.nextTaskId(), moveTaskRequest.sectionId());

        assertThrows(TaskMoveException.class, () -> taskService.moveTask(TASK_ID, moveTaskRequest));

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository).findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository).findByIdAndSectionId(moveTaskRequest.nextTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository, never()).findById(moveTaskRequest.prevTaskId());
        verify(taskRepository, never()).save(any(Task.class));
        verifyNoInteractions(sectionService, activityService, taskPreviewResponseMapper);
    }

    @Test
    void moveTask_whenPrevAndNextTaskIdNull_shouldSetPositionToStep() {
        var moveTaskRequest = new MoveTaskRequest(10L, null, null);
        var task = buildTask(TASK_ID, TASK_TITLE);
        var board = task.getSection().getBoard();
        var newSection = buildSection(10L, "Some Section");
        var taskProperties = mock(AppProperties.Task.class);
        var taskPreviewResponse = buildTaskPreviewResponse(task, List.of());

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(taskProperties).when(appProperties).getTask();
        doReturn(15.0).when(taskProperties).getPositionStep();
        doReturn(newSection).when(sectionService).getProxySectionById(moveTaskRequest.sectionId());
        doReturn(task).when(taskRepository).save(task);
        doReturn(taskPreviewResponse).when(taskPreviewResponseMapper).toDto(task, List.of());

        TaskPreviewResponse actual = taskService.moveTask(TASK_ID, moveTaskRequest);

        assertEquals(taskPreviewResponse, actual);
        assertEquals(15.0, task.getPosition());

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository, never()).findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository, never()).findByIdAndSectionId(moveTaskRequest.nextTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository, never()).findById(moveTaskRequest.prevTaskId());
        verify(taskRepository, never()).findById(moveTaskRequest.nextTaskId());
        verify(taskRepository).save(task);
        verify(activityService).publish(board, TASK_MOVED, "Moved task from section null to Some Section");
        verify(taskPreviewResponseMapper).toDto(task, List.of());
    }

    @Test
    void moveTask_whenOnlyNextProvided_shouldSetPositionBeforeNext() {
        var moveTaskRequest = new MoveTaskRequest(SECTION_ID, null, 20L);
        var task = buildTask(TASK_ID, TASK_TITLE);
        var nextTask = buildTask(20L, "Next task");
        nextTask.setPosition(30.0);
        var taskProperties = mock(AppProperties.Task.class);
        var taskPreviewResponse = buildTaskPreviewResponse(task, List.of());

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(Optional.of(nextTask)).when(taskRepository).findByIdAndSectionId(20L, SECTION_ID);
        doReturn(taskProperties).when(appProperties).getTask();
        doReturn(15.0).when(taskProperties).getPositionStep();
        doReturn(task.getSection()).when(sectionService).getProxySectionById(SECTION_ID);
        doReturn(task).when(taskRepository).save(task);
        doReturn(taskPreviewResponse).when(taskPreviewResponseMapper).toDto(task, List.of());

        TaskPreviewResponse actual = taskService.moveTask(TASK_ID, moveTaskRequest);

        assertEquals(taskPreviewResponse, actual);
        assertEquals(15.0, 30.0 - task.getPosition(), 0.0001);
        assertEquals(15.0, task.getPosition());

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository, never()).findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository).findByIdAndSectionId(moveTaskRequest.nextTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository, never()).findById(moveTaskRequest.prevTaskId());
        verify(taskRepository, never()).findById(moveTaskRequest.nextTaskId());
        verify(taskRepository).save(task);
        verify(taskPreviewResponseMapper).toDto(task, List.of());
        verifyNoInteractions(activityService);
    }

    @Test
    void moveTask_whenOnlyPrevProvided_shouldSetPositionAfterPrev() {
        var moveTaskRequest = new MoveTaskRequest(SECTION_ID, 10L, null);
        var task = buildTask(TASK_ID, TASK_TITLE);
        var prevTask = buildTask(10L, "Prev task");
        prevTask.setPosition(30.0);
        var taskProperties = mock(AppProperties.Task.class);
        var taskPreviewResponse = buildTaskPreviewResponse(task, List.of());

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(Optional.of(prevTask)).when(taskRepository).findByIdAndSectionId(10L, SECTION_ID);
        doReturn(taskProperties).when(appProperties).getTask();
        doReturn(15.0).when(taskProperties).getPositionStep();
        doReturn(task.getSection()).when(sectionService).getProxySectionById(SECTION_ID);
        doReturn(task).when(taskRepository).save(task);
        doReturn(taskPreviewResponse).when(taskPreviewResponseMapper).toDto(task, List.of());

        TaskPreviewResponse actual = taskService.moveTask(TASK_ID, moveTaskRequest);

        assertEquals(taskPreviewResponse, actual);
        assertEquals(45.0, task.getPosition());

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository).findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository, never()).findByIdAndSectionId(moveTaskRequest.nextTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository, never()).findById(moveTaskRequest.prevTaskId());
        verify(taskRepository, never()).findById(moveTaskRequest.nextTaskId());
        verify(taskRepository).save(task);
        verify(taskPreviewResponseMapper).toDto(task, List.of());
        verifyNoInteractions(activityService);
    }

    @Test
    void moveTask_whenPrevAndNextProvidedWithEnoughGap_shouldSetPositionBetween() {
        var taskProperties = mock(AppProperties.Task.class);
        var moveTaskRequest = new MoveTaskRequest(SECTION_ID, 10L, 20L);
        var task = buildTask(TASK_ID, TASK_TITLE);
        var prevTask = buildTask(10L, "Prev task");
        prevTask.setPosition(10.0);
        var nextTask = buildTask(20L, "Next task");
        nextTask.setPosition(30.0);
        var taskPreviewResponse = buildTaskPreviewResponse(task, List.of());

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(Optional.of(prevTask)).when(taskRepository).findByIdAndSectionId(10L, SECTION_ID);
        doReturn(Optional.of(nextTask)).when(taskRepository).findByIdAndSectionId(20L, SECTION_ID);
        doReturn(taskProperties).when(appProperties).getTask();
        doReturn(15.0).when(taskProperties).getPositionStep();
        doReturn(task.getSection()).when(sectionService).getProxySectionById(SECTION_ID);
        doReturn(task).when(taskRepository).save(task);
        doReturn(taskPreviewResponse).when(taskPreviewResponseMapper).toDto(task, List.of());

        TaskPreviewResponse actual = taskService.moveTask(TASK_ID, moveTaskRequest);

        assertEquals(taskPreviewResponse, actual);
        assertEquals(20.0, task.getPosition());

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository).findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository).findByIdAndSectionId(moveTaskRequest.nextTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository, never()).findById(moveTaskRequest.prevTaskId());
        verify(taskRepository, never()).findById(moveTaskRequest.nextTaskId());
        verify(taskRepository).save(task);
        verify(taskPreviewResponseMapper).toDto(task, List.of());
        verifyNoInteractions(activityService);
    }

    @Test
    void moveTask_whenPrevAndNextPositionsTooClose_shouldReindexSectionAndRecalculatePosition() {
        var moveTaskRequest = new MoveTaskRequest(SECTION_ID, 10L, 20L);
        var task = buildTask(TASK_ID, TASK_TITLE);

        var prevTask = buildTask(10L, "Prev task");
        prevTask.setPosition(15.0000001);
        var nextTask = buildTask(20L, "Next task");
        nextTask.setPosition(15.0000002);

        var reindexedPrev = buildTask(10L, "Prev task");
        reindexedPrev.setPosition(15.0);
        var reindexedNext = buildTask(20L, "Next task");
        reindexedNext.setPosition(30.0);

        var otherTask = buildTask(30L, "Other task");
        var taskProperties = mock(AppProperties.Task.class);
        var taskPreviewResponse = buildTaskPreviewResponse(task, List.of());

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(Optional.of(prevTask)).when(taskRepository).findByIdAndSectionId(10L, SECTION_ID);
        doReturn(Optional.of(nextTask)).when(taskRepository).findByIdAndSectionId(20L, SECTION_ID);

        doReturn(List.of(prevTask, nextTask, otherTask)).when(taskRepository)
                .findAllBySectionIdOrderByPositionAsc(SECTION_ID);
        doReturn(taskProperties).when(appProperties).getTask();
        doReturn(15.0).when(taskProperties).getPositionStep();
        doReturn(List.of()).when(taskRepository).saveAll(anyList());

        doReturn(Optional.of(reindexedPrev)).when(taskRepository).findById(10L);
        doReturn(Optional.of(reindexedNext)).when(taskRepository).findById(20L);

        doReturn(task.getSection()).when(sectionService).getProxySectionById(SECTION_ID);
        doReturn(task).when(taskRepository).save(task);
        doReturn(taskPreviewResponse).when(taskPreviewResponseMapper).toDto(task, List.of());

        TaskPreviewResponse actual = taskService.moveTask(TASK_ID, moveTaskRequest);

        assertEquals(taskPreviewResponse, actual);
        assertEquals(22.5, task.getPosition());

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository).findByIdAndSectionId(moveTaskRequest.prevTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository).findByIdAndSectionId(moveTaskRequest.nextTaskId(), moveTaskRequest.sectionId());
        verify(taskRepository).findById(moveTaskRequest.prevTaskId());
        verify(taskRepository).findById(moveTaskRequest.nextTaskId());
        verify(sectionService).getProxySectionById(moveTaskRequest.sectionId());
        verify(taskRepository).findAllBySectionIdOrderByPositionAsc(moveTaskRequest.sectionId());
        verify(taskRepository).saveAll(anyList());
        verify(taskRepository).save(task);
        verify(taskPreviewResponseMapper).toDto(task, List.of());
        verifyNoInteractions(activityService);
    }

    @Test
    void moveTask_whenSectionChanged_shouldPublishTaskMovedActivity() {
        var newSectionId = 99L;
        var moveTaskRequest = new MoveTaskRequest(newSectionId, null, null);
        var task = buildTask(TASK_ID, TASK_TITLE);
        var oldSection = task.getSection();
        var oldBoard = oldSection.getBoard();
        var newSection = buildSection(newSectionId, "New Section");
        var taskProperties = mock(AppProperties.Task.class);
        var taskPreviewResponse = buildTaskPreviewResponse(task, List.of());

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(taskProperties).when(appProperties).getTask();
        doReturn(15.0).when(taskProperties).getPositionStep();
        doReturn(newSection).when(sectionService).getProxySectionById(newSectionId);
        doReturn(task).when(taskRepository).save(task);
        doReturn(taskPreviewResponse).when(taskPreviewResponseMapper).toDto(task, List.of());

        taskService.moveTask(TASK_ID, moveTaskRequest);

        verify(activityService).publish(oldBoard, TASK_MOVED,
                "Moved task from section %s to %s".formatted(oldSection.getName(), "New Section"));
    }

    @Test
    void moveTask_whenSectionUnchanged_shouldNotPublishActivity() {
        var moveTaskRequest = new MoveTaskRequest(SECTION_ID, null, null);
        var task = buildTask(TASK_ID, TASK_TITLE);
        var taskProperties = mock(AppProperties.Task.class);
        var taskPreviewResponse = buildTaskPreviewResponse(task, List.of());

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(taskProperties).when(appProperties).getTask();
        doReturn(15.0).when(taskProperties).getPositionStep();
        doReturn(task.getSection()).when(sectionService).getProxySectionById(SECTION_ID);
        doReturn(task).when(taskRepository).save(task);
        doReturn(taskPreviewResponse).when(taskPreviewResponseMapper).toDto(task, List.of());

        taskService.moveTask(TASK_ID, moveTaskRequest);

        verifyNoInteractions(activityService);
    }

    @Test
    void addLabelToTask_whenTaskNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(taskRepository).findByIdWithLabels(TASK_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskService.addLabelToTask(TASK_ID, LABEL_ID));

        verify(taskRepository).findByIdWithLabels(TASK_ID);
        verifyNoInteractions(labelService, activityService, taskPreviewResponseMapper);
    }

    @Test
    void addLabelToTask_whenLabelLimitExceeded_shouldThrowLabelLimitExceededException() {
        var task = buildTask(TASK_ID, TASK_TITLE);
        task.setLabels(new HashSet<>(Set.of(buildLabel(1L, "Bug"), buildLabel(2L, "Feature"))));
        var labelProperties = mock(AppProperties.Label.class);

        doReturn(Optional.of(task)).when(taskRepository).findByIdWithLabels(TASK_ID);
        doReturn(labelProperties).when(appProperties).getLabel();
        doReturn(2).when(labelProperties).getMaxPerTask();

        assertThrows(LabelLimitExceededException.class, () -> taskService.addLabelToTask(TASK_ID, LABEL_ID));

        verify(taskRepository).findByIdWithLabels(TASK_ID);
        verifyNoInteractions(labelService, activityService, taskPreviewResponseMapper);
    }

    @Test
    void addLabelToTask_whenLabelBelongsToDifferentBoard_shouldThrowCrossBoardOperationException() {
        var task = buildTask(TASK_ID, TASK_TITLE);
        task.setLabels(new HashSet<>());
        var labelProperties = mock(AppProperties.Label.class);
        var otherBoard = getBoard();
        otherBoard.setId(99L);
        var label = buildLabel(LABEL_ID, "Bug");
        label.setBoard(otherBoard);

        doReturn(Optional.of(task)).when(taskRepository).findByIdWithLabels(TASK_ID);
        doReturn(labelProperties).when(appProperties).getLabel();
        doReturn(10).when(labelProperties).getMaxPerTask();
        doReturn(label).when(labelService).getLabelById(LABEL_ID);

        assertThrows(CrossBoardOperationException.class, () -> taskService.addLabelToTask(TASK_ID, LABEL_ID));

        verify(taskRepository).findByIdWithLabels(TASK_ID);
        verify(labelService).getLabelById(LABEL_ID);
        verifyNoInteractions(activityService, taskPreviewResponseMapper);
    }

    @Test
    void addLabelToTask_whenLabelAlreadyAdded_shouldThrowDuplicateException() {
        var task = buildTask(TASK_ID, TASK_TITLE);
        var board = task.getSection().getBoard();
        var label = buildLabel(LABEL_ID, "Bug");
        label.setBoard(board);
        task.setLabels(new HashSet<>(Set.of(label)));

        var labelProperties = mock(AppProperties.Label.class);

        doReturn(Optional.of(task)).when(taskRepository).findByIdWithLabels(TASK_ID);
        doReturn(labelProperties).when(appProperties).getLabel();
        doReturn(10).when(labelProperties).getMaxPerTask();
        doReturn(label).when(labelService).getLabelById(LABEL_ID);

        assertThrows(DuplicateException.class, () -> taskService.addLabelToTask(TASK_ID, LABEL_ID));

        verify(taskRepository).findByIdWithLabels(TASK_ID);
        verify(labelService).getLabelById(LABEL_ID);
        verifyNoInteractions(activityService, taskPreviewResponseMapper);
    }

    @Test
    void addLabelToTask_whenValidRequestData_shouldAddLabelAndReturnTaskPreviewResponse() {
        var task = buildTask(TASK_ID, TASK_TITLE);
        task.setLabels(new HashSet<>());
        var board = task.getSection().getBoard();
        var label = buildLabel(LABEL_ID, "Bug");
        label.setBoard(board);
        var labelProperties = mock(AppProperties.Label.class);
        var taskPreviewResponse = buildTaskPreviewResponse(task, List.of());

        doReturn(Optional.of(task)).when(taskRepository).findByIdWithLabels(TASK_ID);
        doReturn(labelProperties).when(appProperties).getLabel();
        doReturn(10).when(labelProperties).getMaxPerTask();
        doReturn(label).when(labelService).getLabelById(LABEL_ID);
        doReturn(taskPreviewResponse).when(taskPreviewResponseMapper).toDto(task, List.of());

        TaskPreviewResponse actual = taskService.addLabelToTask(TASK_ID, LABEL_ID);

        assertEquals(taskPreviewResponse, actual);
        assertTrue(task.getLabels().contains(label));

        verify(taskRepository).findByIdWithLabels(TASK_ID);
        verify(labelService).getLabelById(LABEL_ID);
        verify(activityService).publish(board, TASK_LABEL_ADDED,
                "Added label %s to task %s".formatted(label.getName(), task.getTitle()));
        verify(taskPreviewResponseMapper).toDto(task, List.of());
    }

    @Test
    void deleteLabelFromTask_whenTaskNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(taskRepository).findByIdWithLabels(TASK_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskService.deleteLabelFromTask(TASK_ID, LABEL_ID));

        verify(taskRepository).findByIdWithLabels(TASK_ID);
        verifyNoInteractions(labelService, activityService, taskPreviewResponseMapper);
    }

    @Test
    void deleteLabelFromTask_whenLabelBelongsToDifferentBoard_shouldThrowCrossBoardOperationException() {
        var task = buildTask(TASK_ID, TASK_TITLE);
        Board otherBoard = getBoard();
        otherBoard.setId(99L);
        var label = buildLabel(LABEL_ID, "Bug");
        label.setBoard(otherBoard);

        doReturn(Optional.of(task)).when(taskRepository).findByIdWithLabels(TASK_ID);
        doReturn(label).when(labelService).getLabelById(LABEL_ID);

        assertThrows(CrossBoardOperationException.class, () -> taskService.deleteLabelFromTask(TASK_ID, LABEL_ID));

        verify(taskRepository).findByIdWithLabels(TASK_ID);
        verify(labelService).getLabelById(LABEL_ID);
        verifyNoInteractions(activityService, taskPreviewResponseMapper);
    }

    @Test
    void deleteLabelFromTask_whenValidRequestData_shouldRemoveLabelAndReturnTaskPreviewResponse() {
        var task = buildTask(TASK_ID, TASK_TITLE);
        var board = task.getSection().getBoard();
        var label = buildLabel(LABEL_ID, "Bug");
        label.setBoard(board);
        task.setLabels(new HashSet<>(Set.of(label)));
        var taskPreviewResponse = buildTaskPreviewResponse(task, List.of());

        doReturn(Optional.of(task)).when(taskRepository).findByIdWithLabels(TASK_ID);
        doReturn(label).when(labelService).getLabelById(LABEL_ID);
        doReturn(taskPreviewResponse).when(taskPreviewResponseMapper).toDto(task, List.of());

        TaskPreviewResponse actual = taskService.deleteLabelFromTask(TASK_ID, LABEL_ID);

        assertEquals(taskPreviewResponse, actual);
        assertFalse(task.getLabels().contains(label));

        verify(taskRepository).findByIdWithLabels(TASK_ID);
        verify(labelService).getLabelById(LABEL_ID);
        verify(activityService).publish(board, TASK_LABEL_DELETED,
                "Removed label %s from task %s".formatted(label.getName(), task.getTitle()));
        verify(taskPreviewResponseMapper).toDto(task, List.of());
    }

    private Label buildLabel(Long id, String name) {
        Label label = new Label();
        label.setId(id);
        label.setName(name);

        return label;
    }

    private Board getBoard() {
        Board board = new Board();
        board.setId(BOARD_ID);

        return board;
    }

    private Section buildSection(Long sectionId, String name) {
        Section section = new Section();
        section.setId(sectionId);
        section.setName(name);
        section.setBoard(getBoard());

        return section;
    }

    private Section getSection() {
        Section section = new Section();
        section.setId(SECTION_ID);
        section.setBoard(getBoard());

        return section;
    }

    private Task buildTask(Long id, String title) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setSection(getSection());

        return task;
    }

    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);

        return user;
    }

    private TaskAttachment buildAttachment(Task task, String filePath) {
        TaskAttachment taskAttachment = new TaskAttachment();
        taskAttachment.setFilePath(filePath);
        taskAttachment.setTask(task);

        return taskAttachment;
    }

    private UserShortResponse buildUserShortResponse(User user) {
        return new UserShortResponse(user.getId(), user.getUsername(), "", "");
    }

    private TaskPreviewResponse buildTaskPreviewResponse(Task task, List<UserShortResponse> assignees) {
        return new TaskPreviewResponse(
                task.getId(),
                task.getTitle(),
                task.getIsCompleted(),
                task.getDueDate(),
                task.getSection().getId(),
                List.of(),
                assignees,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private TaskFullResponse buildTaskFullResponse(Task task, List<UserShortResponse> assignees) {
        return new TaskFullResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getIsCompleted(),
                task.getDueDate(),
                task.getSection().getId(),
                List.of(),
                assignees,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private CreateTaskRequest buildCreateTaskRequest(Set<Long> assigneeIds) {
        return new CreateTaskRequest(TASK_TITLE, "Description", null, assigneeIds, SECTION_ID);
    }

    private TaskFilter buildEmptyTaskFilter() {
        return new TaskFilter(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
