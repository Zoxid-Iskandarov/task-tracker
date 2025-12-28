package com.walking.backend.integration.service;

import com.walking.backend.domain.dto.task.TaskRequest;
import com.walking.backend.domain.dto.task.TaskResponse;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.integration.annotation.WithMockUser;
import com.walking.backend.repository.TaskRepository;
import com.walking.backend.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser
@RequiredArgsConstructor
public class TaskServiceIT extends IntegrationTestBase {
    private final TaskService taskService;
    private final TaskRepository taskRepository;

    private static final Long TASK_ID = 1L;
    private static final Long USER_ID = 1L;

    @Test
    void getTasks_whenOnlyUserIdProvided_returnTasks() {
        Page<TaskResponse> tasks = taskService
                .getTasks(USER_ID, null, null, PageRequest.of(0, 5));

        assertThat(tasks).isNotNull();
        assertThat(tasks.getTotalElements()).isEqualTo(5L);
        assertThat(tasks.getContent()).hasSize(5);
    }

    @Test
    void getTasks_whenCompetedTrue_returnOnlyCompletedTasks() {
        Page<TaskResponse> tasks = taskService
                .getTasks(USER_ID, true, null, PageRequest.of(0, 5));

        assertThat(tasks).isNotNull();
        assertThat(tasks.getContent()).hasSize(3);

        tasks.forEach(task -> assertThat(task.isCompleted()).isTrue());
    }

    @Test
    void getTasks_whenCompletedFalse_returnOnlyCompletedTasks() {
        Page<TaskResponse> tasks = taskService
                .getTasks(USER_ID, false, null, PageRequest.of(0, 5));

        assertThat(tasks).isNotNull();
        assertThat(tasks.getContent()).hasSize(2);

        tasks.forEach(task -> assertThat(task.isCompleted()).isFalse());
    }

    @Test
    void getTasks_whenTodayTrue_returnOnlyTodayCreatedTasks() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1L).toLocalDate().atStartOfDay();

        Page<TaskResponse> tasks = taskService
                .getTasks(USER_ID, null, true, PageRequest.of(0, 5));

        assertThat(tasks).isNotNull();
        assertThat(tasks.getContent()).hasSize(3);

        tasks.forEach(task -> assertThat(task.created().isAfter(yesterday)).isTrue());
    }

    @Test
    void getTasks_whenCompletedAndTodayProvided_returnFilteredTasks() {
        LocalDate today = LocalDate.now();

        Page<TaskResponse> tasks = taskService
                .getTasks(USER_ID, true, true, PageRequest.of(0, 5));

        assertThat(tasks).isNotNull();
        assertThat(tasks.getContent()).hasSize(2);

        tasks.forEach(task -> {
            assertThat(task.isCompleted()).isTrue();
            assertThat(task.created().toLocalDate()).isEqualTo(today);
        });
    }

    @Test
    void createTask_whenValidRequestData_returnTaskResponse() {
        TaskRequest taskRequest = new TaskRequest("New TITLE", "New DESCRIPTION");

        TaskResponse taskResponse = taskService.createTask(taskRequest, USER_ID);

        assertThat(taskRepository.findById(taskResponse.id()).isPresent()).isTrue();
        assertThat(taskResponse.title()).isEqualTo(taskRequest.title());
        assertThat(taskResponse.description()).isEqualTo(taskRequest.description());
    }

    @Test
    void createTask_whenUserNotFound_throwObjectNotFoundException() {
        TaskRequest taskRequest = new TaskRequest("New TITLE", "New DESCRIPTION");

        assertThatThrownBy(() -> taskService.createTask(taskRequest, 1000L))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("User with id '%d' not found".formatted(1000L));
    }

    @Test
    void updateTask_whenValidRequestData_returnTaskResponse() {
        TaskResponse beforeUpdate = taskService
                .getTasks(USER_ID, null, null, PageRequest.of(0, 1))
                .getContent()
                .getFirst();
        TaskRequest taskRequest = new TaskRequest("Something TITLE", "Something DESCRIPTION");

        TaskResponse taskResponse = taskService.updateTask(1L, taskRequest);

        assertThat(taskResponse.title()).isNotEqualTo(beforeUpdate.title());
        assertThat(taskResponse.description()).isNotEqualTo(beforeUpdate.description());

        assertThat(taskResponse.id()).isEqualTo(beforeUpdate.id());
        assertThat(taskResponse.isCompleted()).isEqualTo(beforeUpdate.isCompleted());
    }

    @Test
    void updateTask_whenUserIsNotOwner_throwAccessDeniedException() {
        TaskRequest taskRequest = new TaskRequest("Something TITLE", "Something DESCRIPTION");

        assertThatThrownBy(() -> taskService.updateTask(6L, taskRequest))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void toggleCompleted_whenTaskExists_returnTaskResponse() {
        TaskResponse beforeToggle = taskService
                .getTasks(USER_ID, null, null, PageRequest.of(0, 1))
                .getContent()
                .getFirst();
        TaskResponse taskResponse = taskService.toggleCompleted(TASK_ID);

        assertThat(taskResponse).isNotNull();
        assertThat(taskResponse.isCompleted()).isNotEqualTo(beforeToggle.isCompleted());
        assertThat(taskResponse.id()).isEqualTo(beforeToggle.id());
        assertThat(taskResponse.title()).isEqualTo(beforeToggle.title());
        assertThat(taskResponse.description()).isEqualTo(beforeToggle.description());
    }

    @Test
    void toggleCompleted_whenUserIsNotOwner_throwAccessDeniedException() {
        assertThatThrownBy(() -> taskService.toggleCompleted(6L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteTask_whenTaskExists_success() {
        assertThat(taskRepository.findById(TASK_ID).isEmpty()).isFalse();

        taskService.deleteTask(TASK_ID);

        assertThat(taskRepository.findById(TASK_ID).isEmpty()).isTrue();
    }

    @Test
    void deleteTask_whenUserIsNotOwner_throwAccessDeniedException() {
        assertThatThrownBy(() -> taskService.deleteTask(6L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
