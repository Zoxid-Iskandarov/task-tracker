package com.walking.backend.integration.service;

import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.label.CreateLabelRequest;
import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.dto.task.*;
import com.walking.backend.domain.exception.*;
import com.walking.backend.domain.model.Section;
import com.walking.backend.domain.model.Task;
import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.integration.annotation.WithMockUser;
import com.walking.backend.props.AppProperties;
import com.walking.backend.repository.SectionRepository;
import com.walking.backend.repository.TaskRepository;
import com.walking.backend.service.LabelService;
import com.walking.backend.service.TaskService;
import com.walking.backend.storage.service.ResourceCleanupService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Set;

import static com.walking.backend.domain.model.ActivityType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@WithMockUser
@RequiredArgsConstructor
public class TaskServiceIT extends IntegrationTestBase {
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final SectionRepository sectionRepository;
    private final LabelService labelService;
    private final ActivityService activityService;
    private final ResourceCleanupService resourceCleanupService;
    private final AppProperties appProperties;

    @Test
    void getTasks_whenSectionExistsAndUserHasAccess_shouldReturnTasksPage() {
        var pageable = PageRequest.of(0, 10);

        Page<TaskPreviewResponse> actual = taskService.getTasks(1L, pageable);

        assertThat(actual.getContent()).isNotEmpty();
        assertThat(actual.getContent())
                .extracting(TaskPreviewResponse::title)
                .contains("Test Task With Two Assignees");
    }

    @Test
    void getTasks_whenSectionHasNoTasks_shouldReturnEmptyPage() {
        var pageable = PageRequest.of(0, 10);

        Page<TaskPreviewResponse> actual = taskService.getTasks(2L, pageable);

        assertThat(actual.getContent()).isEmpty();
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void getTasks_whenUserHasNoAccess_shouldThrowAccessDeniedException() {
        var pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> taskService.getTasks(1L, pageable))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void searchTasks_whenValidFilterAndUserHasAccess_shouldReturnFilteredTasksPage() {
        var filter = new TaskFilter("Test", 1L, false, null, null, null, null, null, null);
        var pageable = PageRequest.of(0, 10);

        Page<TaskPreviewResponse> actual = taskService.searchTasks(1L, filter, pageable);

        assertThat(actual.getContent()).isNotEmpty();
    }

    @Test
    void searchTasks_whenNoTasksMatchFilter_shouldReturnEmptyPage() {
        var filter = new TaskFilter("Nonexistent Task Title", 1L, null, null, null, null, null, null, null);
        var pageable = PageRequest.of(0, 10);

        Page<TaskPreviewResponse> actual = taskService.searchTasks(1L, filter, pageable);

        assertThat(actual.getContent()).isEmpty();
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void searchTasks_whenUserHasNoAccess_shouldThrowAccessDeniedException() {
        var filter = new TaskFilter(null, null, null, null, null, null, null, null, null);
        var pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> taskService.searchTasks(1L, filter, pageable))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getTaskById_whenTaskExistsAndUserHasAccess_shouldReturnTaskFullResponse() {
        TaskFullResponse response = taskService.getTaskById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Test Task With Two Assignees");
    }

    @Test
    void getTaskById_whenTaskNotFound_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void getTaskById_whenUserHasNoAccess_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> taskService.getTaskById(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createTask_whenValidRequest_shouldCreateTask() {
        var request = new CreateTaskRequest("New Task Title", "New Description", null, Set.of(2L), 1L);

        TaskFullResponse response = taskService.createTask(request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("New Task Title");
        assertThat(response.description()).isEqualTo("New Description");

        Task saved = taskRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("New Task Title");
    }

    @Test
    void createTask_whenFirstTaskInSection_shouldSetPositionToStep() {
        var request = new CreateTaskRequest("First Task", "Desc", null, null, 2L);

        TaskFullResponse response = taskService.createTask(request);

        Task saved = taskRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getPosition()).isEqualTo(appProperties.getTask().getPositionStep());
    }

    @Test
    void createTask_whenTasksExistInSection_shouldSetPositionAfterLastTask() {
        var request = new CreateTaskRequest("Fourth Task", "Desc", null, null, 1L);

        TaskFullResponse response = taskService.createTask(request);

        Task saved = taskRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getPosition()).isGreaterThan(3.0);
    }

    @Test
    void createTask_whenAssigneeNotBoardMember_shouldThrowInvalidTaskAssigneeException() {
        var request = new CreateTaskRequest("New Task", "Desc", null, Set.of(99L), 1L);

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(InvalidTaskAssigneeException.class);
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe")
    void createTask_whenUserCannotEditSection_shouldThrowAccessDeniedException() {
        var request = new CreateTaskRequest("New Task", "Desc", null, null, 1L);

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateTask_whenValidRequest_shouldUpdateTaskAndPublishActivity() {
        var request = new UpdateTaskRequest("Renamed Task", "Updated desc", LocalDateTime.now(), Set.of(2L));

        TaskFullResponse response = taskService.updateTask(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Renamed Task");

        verify(activityService).publish(any(), eq(TASK_UPDATED), eq("Renamed task from Test Task With Two Assignees to Renamed Task"));
    }

    @Test
    void updateTask_whenOnlyDescriptionChanged_shouldPublishUpdatedActivity() {
        var request = new UpdateTaskRequest("Test Task With Two Assignees", "Only description changed", LocalDateTime.now(), Set.of(2L));

        taskService.updateTask(request, 1L);

        verify(activityService).publish(any(), eq(TASK_UPDATED), eq("Updated task Test Task With Two Assignees"));
    }

    @Test
    void updateTask_whenInvalidAssignee_shouldThrowInvalidTaskAssigneeException() {
        var request = new UpdateTaskRequest("Task", "Desc", null, Set.of(99L));

        assertThatThrownBy(() -> taskService.updateTask(request, 1L))
                .isInstanceOf(InvalidTaskAssigneeException.class);
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe")
    void updateTask_whenUserCannotEdit_shouldThrowAccessDeniedException() {
        var request = new UpdateTaskRequest("Task", "Desc", null, null);

        assertThatThrownBy(() -> taskService.updateTask(request, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateTask_whenTaskNotFound_shouldThrowAccessDeniedException() {
        var request = new UpdateTaskRequest("Task", "Desc", null, null);

        assertThatThrownBy(() -> taskService.updateTask(request, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteTask_whenValidAndUserCanEdit_shouldDeleteTaskAndCleanupFilesAndPublishActivity() {
        taskService.deleteTask(1L);

        assertThat(taskRepository.findById(1L)).isEmpty();

        verify(activityService).publish(any(), eq(TASK_DELETED), eq("Deleted task Test Task With Two Assignees"));
        verify(resourceCleanupService).cleanupFiles(anyList());
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe")
    void deleteTask_whenUserCannotEdit_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> taskService.deleteTask(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteTask_whenTaskNotFound_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void toggleCompleted_whenTaskIncomplete_shouldMarkAsCompletedAndPublishTaskCompleted() {
        TaskPreviewResponse response = taskService.toggleCompleted(1L);

        assertThat(response).isNotNull();
        assertThat(response.isCompleted()).isTrue();

        verify(activityService).publish(any(), eq(TASK_COMPLETED), eq("Completed task Test Task With Two Assignees"));
    }

    @Test
    void toggleCompleted_whenTaskComplete_shouldMarkAsIncompleteAndPublishTaskReopened() {
        taskService.toggleCompleted(1L);

        TaskPreviewResponse response = taskService.toggleCompleted(1L);

        assertThat(response.isCompleted()).isFalse();

        verify(activityService).publish(any(), eq(TASK_REOPENED), eq("Reopened task Test Task With Two Assignees"));
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void toggleCompleted_whenUserCannotToggle_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> taskService.toggleCompleted(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void toggleCompleted_whenTaskNotFound_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> taskService.toggleCompleted(99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void moveTask_whenMovingToAnotherSection_shouldChangeSectionAndPublishActivity() {
        Section targetSection = sectionRepository.findById(2L).orElseThrow();
        var request = new MoveTaskRequest(2L, null, null);

        TaskPreviewResponse response = taskService.moveTask(1L, request);

        assertThat(response).isNotNull();

        verify(activityService).publish(any(), eq(TASK_MOVED), eq("Moved task from section To Do to " + targetSection.getName()));
    }

    @Test
    void moveTask_whenMovingWithinSection_shouldUpdatePosition() {
        var request = new MoveTaskRequest(1L, null, 2L); // move task 1 before task 2

        TaskPreviewResponse response = taskService.moveTask(1L, request);

        assertThat(response).isNotNull();
    }

    @Test
    void moveTask_whenPrevAndNextAreSame_shouldThrowTaskMoveException() {
        var request = new MoveTaskRequest(1L, 2L, 2L);

        assertThatThrownBy(() -> taskService.moveTask(1L, request))
                .isInstanceOf(TaskMoveException.class);
    }

    @Test
    void moveTask_whenMovingRelativeToItself_shouldThrowTaskMoveException() {
        var request = new MoveTaskRequest(1L, 1L, null);

        assertThatThrownBy(() -> taskService.moveTask(1L, request))
                .isInstanceOf(TaskMoveException.class);
    }

    @Test
    void moveTask_whenPrevTaskNotInTargetSection_shouldThrowTaskMoveException() {
        var request = new MoveTaskRequest(1L, 99L, null);

        assertThatThrownBy(() -> taskService.moveTask(1L, request))
                .isInstanceOf(TaskMoveException.class);
    }

    @Test
    void moveTask_whenNextTaskNotInTargetSection_shouldThrowTaskMoveException() {
        var request = new MoveTaskRequest(1L, null, 99L);

        assertThatThrownBy(() -> taskService.moveTask(1L, request))
                .isInstanceOf(TaskMoveException.class);
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe")
    void moveTask_whenUserCannotEditTask_shouldThrowAccessDeniedException() {
        var request = new MoveTaskRequest(2L, null, null);

        assertThatThrownBy(() -> taskService.moveTask(1L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addLabelToTask_whenValid_shouldAddLabelAndPublishActivity() {
        TaskPreviewResponse response = taskService.addLabelToTask(1L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.labels()).hasSize(1);

        verify(activityService).publish(any(), eq(TASK_LABEL_ADDED), eq("Added label Bug to task Test Task With Two Assignees"));
    }

    @Test
    void addLabelToTask_whenLabelLimitExceeded_shouldThrowLabelLimitExceededException() {
        int maxLabels = appProperties.getLabel().getMaxPerTask();

        for (int i = 0; i < maxLabels; i++) {
            CreateLabelRequest request = new CreateLabelRequest("label-%d".formatted(i), "black", 1L);

            LabelResponse label = labelService.createLabel(request);
            taskService.addLabelToTask(1L, label.id());
        }

        assertThatThrownBy(() -> taskService.addLabelToTask(1L, 1L))
                .isInstanceOf(LabelLimitExceededException.class)
                .hasMessageContaining("Task cannot contain more than");
    }

    @Test
    void addLabelToTask_whenLabelFromDifferentBoard_shouldThrowCrossBoardOperationException() {
        assertThatThrownBy(() -> taskService.addLabelToTask(1L, 3L))
                .isInstanceOf(CrossBoardOperationException.class)
                .hasMessage("Task and label must belong to the same board");
    }

    @Test
    void addLabelToTask_whenLabelAlreadyAdded_shouldThrowDuplicateException() {
        taskService.addLabelToTask(1L, 1L);

        assertThatThrownBy(() -> taskService.addLabelToTask(1L, 1L))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe")
    void addLabelToTask_whenUserCannotEditTask_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> taskService.addLabelToTask(1L, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteLabelFromTask_whenValid_shouldRemoveLabelAndPublishActivity() {
        taskService.addLabelToTask(1L, 1L);

        TaskPreviewResponse response = taskService.deleteLabelFromTask(1L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.labels()).isEmpty();

        verify(activityService).publish(any(), eq(TASK_LABEL_DELETED), eq("Removed label Bug from task Test Task With Two Assignees"));
    }

    @Test
    void deleteLabelFromTask_whenLabelFromDifferentBoard_shouldThrowCrossBoardOperationException() {
        assertThatThrownBy(() -> taskService.deleteLabelFromTask(1L, 3L))
                .isInstanceOf(CrossBoardOperationException.class);
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe")
    void deleteLabelFromTask_whenUserCannotEditTask_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> taskService.deleteLabelFromTask(1L, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
