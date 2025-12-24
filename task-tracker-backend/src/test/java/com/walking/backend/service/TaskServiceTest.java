package com.walking.backend.service;

import com.walking.backend.domain.dto.task.TaskRequest;
import com.walking.backend.domain.dto.task.TaskResponse;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Task;
import com.walking.backend.domain.model.User;
import com.walking.backend.repository.TaskRepository;
import com.walking.backend.service.impl.TaskServiceImpl;
import com.walking.backend.service.mapper.task.TaskRequestMapper;
import com.walking.backend.service.mapper.task.TaskResponseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {
    private static final Long USER_ID = 1L;
    private static final Long TASK_ID = 1L;
    private static final String TASK_TITLE = "Title";
    private static final String TASK_DESCRIPTION = "Description";

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserService userService;

    @Mock
    private TaskRequestMapper taskRequestMapper;

    @Mock
    private TaskResponseMapper taskResponseMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void getTasks_whenOnlyUserIdProvided_returnTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Task task1 = getTask(1L);
        Task task2 = getTask(2L);

        Page<Task> page = new PageImpl<>(List.of(task1, task2), pageable, 2L);

        doReturn(page).when(taskRepository).findAll(any(Specification.class), eq(pageable));
        doReturn(getTaskResponse(task1)).when(taskResponseMapper).toDto(task1);
        doReturn(getTaskResponse(task2)).when(taskResponseMapper).toDto(task2);

        Page<TaskResponse> actual = taskService.getTasks(USER_ID, null, null, pageable);

        assertNotNull(actual);
        assertEquals(2, actual.getTotalElements());
        assertEquals(2, actual.getContent().size());

        verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        verify(taskResponseMapper).toDto(task1);
        verify(taskResponseMapper).toDto(task2);
        verifyNoMoreInteractions(taskRepository, taskResponseMapper);
    }

    @Test
    void getTasks_whenCompetedTrue_returnOnlyCompletedTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Task task1 = getTask(1L);
        task1.setIsCompleted(true);
        Task task2 = getTask(2L);
        task2.setIsCompleted(true);

        Page<Task> page = new PageImpl<>(List.of(task1, task2), pageable, 2L);

        doReturn(page).when(taskRepository).findAll(any(Specification.class), eq(pageable));
        doReturn(getTaskResponse(task1)).when(taskResponseMapper).toDto(task1);
        doReturn(getTaskResponse(task2)).when(taskResponseMapper).toDto(task2);

        Page<TaskResponse> actual = taskService.getTasks(USER_ID, true, null, pageable);

        assertNotNull(actual);
        assertEquals(2, actual.getTotalElements());
        assertEquals(2, actual.getContent().size());

        actual.getContent().forEach(task ->
                assertTrue(task.isCompleted())
        );

        verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        verify(taskResponseMapper).toDto(task1);
        verify(taskResponseMapper).toDto(task2);
        verifyNoMoreInteractions(taskRepository, taskResponseMapper);
    }

    @Test
    void getTasks_whenCompetedFalse_returnOnlyCompletedTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Task task1 = getTask(1L);
        Task task2 = getTask(2L);

        Page<Task> page = new PageImpl<>(List.of(task1, task2), pageable, 2L);

        doReturn(page).when(taskRepository).findAll(any(Specification.class), eq(pageable));
        doReturn(getTaskResponse(task1)).when(taskResponseMapper).toDto(task1);
        doReturn(getTaskResponse(task2)).when(taskResponseMapper).toDto(task2);

        Page<TaskResponse> actual = taskService.getTasks(USER_ID, false, null, pageable);

        assertNotNull(actual);
        assertEquals(2, actual.getTotalElements());
        assertEquals(2, actual.getContent().size());
        actual.getContent().forEach(task ->
                assertFalse(task.isCompleted())
        );

        verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        verify(taskResponseMapper).toDto(task1);
        verify(taskResponseMapper).toDto(task2);
        verifyNoMoreInteractions(taskRepository, taskResponseMapper);
    }

    @Test
    void getTasks_whenTodayTrue_returnOnlyTodayTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Task task1 = getTask(1L);
        Task task2 = getTask(2L);

        Page<Task> page = new PageImpl<>(List.of(task1, task2), pageable, 2L);

        doReturn(page).when(taskRepository).findAll(any(Specification.class), eq(pageable));
        doReturn(getTaskResponse(task1)).when(taskResponseMapper).toDto(task1);
        doReturn(getTaskResponse(task2)).when(taskResponseMapper).toDto(task2);

        Page<TaskResponse> actual = taskService.getTasks(USER_ID, null, true, pageable);

        assertNotNull(actual);
        assertEquals(2, actual.getTotalElements());
        assertEquals(2, actual.getContent().size());

        verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        verify(taskResponseMapper).toDto(task1);
        verify(taskResponseMapper).toDto(task2);
        verifyNoMoreInteractions(taskRepository, taskResponseMapper);
    }

    @Test
    void getTasks_whenCompletedAndTodayProvided_returnFilteredTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Task task1 = getTask(1L);
        Task task2 = getTask(2L);
        LocalDate today = LocalDate.now();

        Page<Task> page = new PageImpl<>(List.of(task1, task2), pageable, 2L);

        doReturn(page).when(taskRepository).findAll(any(Specification.class), eq(pageable));
        doReturn(getTaskResponse(task1)).when(taskResponseMapper).toDto(task1);
        doReturn(getTaskResponse(task2)).when(taskResponseMapper).toDto(task2);

        Page<TaskResponse> actual = taskService.getTasks(USER_ID, false, true, pageable);

        assertNotNull(actual);
        assertEquals(2, actual.getTotalElements());
        assertEquals(2, actual.getContent().size());
        actual.getContent()
                .forEach(task -> {
                    assertFalse(task.isCompleted());
                    assertEquals(today, task.created().toLocalDate());
                }
        );

        verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        verify(taskResponseMapper).toDto(task1);
        verify(taskResponseMapper).toDto(task2);
        verifyNoMoreInteractions(taskRepository, taskResponseMapper);
    }

    @Test
    void getTasks_whenNoTasksFound_returnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Task> page = new PageImpl<>(List.of(), pageable, 0L);

        doReturn(page).when(taskRepository).findAll(any(Specification.class), eq(pageable));

        Page<TaskResponse> actual = taskService.getTasks(USER_ID, true, true, pageable);

        assertNotNull(actual);
        assertEquals(0, actual.getTotalElements());
        assertEquals(0, actual.getContent().size());

        verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoMoreInteractions(taskRepository);
        verifyNoInteractions(taskResponseMapper);
    }

    @Test
    void getTasks_whenPageableProvided_passItToRepository() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Task> emptyPage = Page.empty(pageable);

        doReturn(emptyPage).when(taskRepository).findAll(any(Specification.class), eq(pageable));

        Page<TaskResponse> actual = taskService.getTasks(USER_ID, null, null, pageable);

        assertNotNull(actual);
        assertThat(actual.getContent()).isEmpty();

        verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoInteractions(taskResponseMapper);
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void createTask_whenValidRequestData_returnTaskResponse() {
        TaskRequest taskRequest = getTaskRequest();
        Task unSavedTask = getUnSavedTask();
        Task savedTask = getSavedTask();
        User user = getUser();
        TaskResponse expected = getTaskResponse(taskRequest.title(), taskRequest.description(), false);

        doReturn(unSavedTask).when(taskRequestMapper).toEntity(taskRequest);
        doReturn(user).when(userService).getUserById(USER_ID);
        doReturn(savedTask).when(taskRepository).save(unSavedTask);
        doReturn(expected).when(taskResponseMapper).toDto(savedTask);

        TaskResponse actual = taskService.createTask(taskRequest, USER_ID);

        assertEquals(expected.id(), actual.id());
        assertEquals(expected.title(), actual.title());
        assertEquals(expected.description(), actual.description());
        assertEquals(expected.isCompleted(), actual.isCompleted());
        assertEquals(expected.created(), actual.created());
        assertEquals(expected.updated(), actual.updated());

        verify(taskRequestMapper).toEntity(taskRequest);
        verify(userService).getUserById(USER_ID);
        verify(taskRepository).save(unSavedTask);
        verify(taskResponseMapper).toDto(savedTask);
        verifyNoMoreInteractions(taskRequestMapper, userService, taskRepository, taskResponseMapper);
    }

    @Test
    void createTask_whenUserNotFound_throwObjectNotFoundException() {
        TaskRequest taskRequest = getTaskRequest();
        Task unSavedTask = getUnSavedTask();

        doReturn(unSavedTask).when(taskRequestMapper).toEntity(taskRequest);
        doThrow(ObjectNotFoundException.class).when(userService).getUserById(USER_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskService.createTask(taskRequest, USER_ID));

        verify(taskRequestMapper).toEntity(taskRequest);
        verify(userService).getUserById(USER_ID);
        verify(taskRepository, never()).save(any(Task.class));
        verify(taskResponseMapper, never()).toDto(any(Task.class));
        verifyNoMoreInteractions(taskRequestMapper, userService, taskRepository, taskResponseMapper);
    }

    @Test
    void updateTask_whenTaskExists_returnTaskResponse() {
        TaskRequest taskRequest = getTaskRequest("UPDATED title", "UPDATED description");
        Task task = getSavedTask();
        TaskResponse expected = getTaskResponse(taskRequest.title(), taskRequest.description(), false);

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(task).when(taskRepository).save(task);
        doReturn(expected).when(taskResponseMapper).toDto(task);

        TaskResponse actual = taskService.updateTask(TASK_ID, taskRequest);

        assertEquals(expected.id(), actual.id());
        assertEquals(expected.title(), actual.title());
        assertEquals(expected.description(), actual.description());
        assertEquals(expected.isCompleted(), actual.isCompleted());
        assertEquals(expected.created(), actual.created());
        assertEquals(expected.updated(), actual.updated());

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository).save(task);
        verify(taskResponseMapper).toDto(task);
        verifyNoMoreInteractions(taskRepository, taskResponseMapper);
    }

    @Test
    void updateTask_whenTaskNotFound_throwObjectNotFoundException() {
        doReturn(Optional.empty()).when(taskRepository).findById(TASK_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskService.updateTask(TASK_ID, getTaskRequest()));

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository, never()).save(any(Task.class));
        verify(taskResponseMapper, never()).toDto(any(Task.class));
        verifyNoMoreInteractions(taskRepository, taskResponseMapper);
    }

    @Test
    void toggleCompleted_whenTaskExists_returnTaskResponse() {
        Task task = getSavedTask();
        TaskResponse expected = getTaskResponse();

        doReturn(Optional.of(task)).when(taskRepository).findById(TASK_ID);
        doReturn(task).when(taskRepository).save(task);
        doReturn(expected).when(taskResponseMapper).toDto(task);

        TaskResponse actual = taskService.toggleCompleted(TASK_ID);

        assertNotNull(actual);
        assertEquals(expected.id(), actual.id());
        assertEquals(expected.title(), actual.title());
        assertEquals(expected.description(), actual.description());
        assertEquals(expected.isCompleted(), actual.isCompleted());
        assertTrue(actual.isCompleted());
        assertEquals(expected.created(), actual.created());
        assertEquals(expected.updated(), actual.updated());

        verify(taskRepository).findById(expected.id());
        verify(taskRepository).save(task);
        verify(taskResponseMapper).toDto(task);
        verifyNoMoreInteractions(taskRepository, taskResponseMapper);
    }

    @Test
    void toggleCompeted_whenTaskNotFound_throwObjectNotFoundException() {
        doReturn(Optional.empty()).when(taskRepository).findById(TASK_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskService.toggleCompleted(TASK_ID));

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository, never()).save(any(Task.class));
        verify(taskResponseMapper, never()).toDto(any(Task.class));
        verifyNoMoreInteractions(taskRepository, taskResponseMapper);
    }

    @Test
    void deleteTask_whenTaskExists_success() {
        Task task = getSavedTask();

        doReturn(Optional.of(task)).when(taskRepository).findById(task.getId());
        doNothing().when(taskRepository).delete(task);

        taskService.deleteTask(task.getId());

        verify(taskRepository).findById(task.getId());
        verify(taskRepository).delete(task);
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void deleteTask_whenTaskNotFound_throwObjectNotFoudException() {
        doReturn(Optional.empty()).when(taskRepository).findById(TASK_ID);

        assertThrows(ObjectNotFoundException.class, () -> taskService.deleteTask(TASK_ID));

        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository, never()).delete(any(Task.class));
        verifyNoMoreInteractions(taskRepository);
    }

    private Task getUnSavedTask() {
        Task task = new Task();
        task.setTitle(TASK_TITLE);
        task.setDescription(TASK_DESCRIPTION);
        task.setIsCompleted(false);

        return task;
    }

    private Task getSavedTask() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTitle(TASK_TITLE);
        task.setDescription(TASK_DESCRIPTION);
        task.setIsCompleted(false);
        task.setCreated(LocalDateTime.now());
        task.setUpdated(LocalDateTime.now());

        return task;
    }

    private TaskResponse getTaskResponse(String title, String description, boolean isCompleted) {
        return new TaskResponse(TASK_ID, title, description, isCompleted, LocalDateTime.now(), LocalDateTime.now());
    }

    private TaskResponse getTaskResponse() {
        return getTaskResponse(TASK_TITLE, TASK_DESCRIPTION, true);
    }

    private TaskRequest getTaskRequest(String title, String description) {
        return new TaskRequest(title, description);
    }

    private TaskRequest getTaskRequest() {
        return getTaskRequest(TASK_TITLE, TASK_DESCRIPTION);
    }

    private User getUser() {
        User user = new User();
        user.setId(USER_ID);

        return user;
    }

    private Task getTask(Long id) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("Title '%d'".formatted(id));
        task.setDescription("Description '%d'".formatted(id));
        task.setIsCompleted(false);
        task.setCreated(LocalDateTime.now());
        task.setUpdated(LocalDateTime.now());

        return task;
    }

    private TaskResponse getTaskResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getIsCompleted(),
                task.getCreated(),
                task.getUpdated()
        );
    }
}
