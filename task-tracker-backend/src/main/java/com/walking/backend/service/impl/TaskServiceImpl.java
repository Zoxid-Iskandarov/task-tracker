package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.task.TaskRequest;
import com.walking.backend.domain.dto.task.TaskResponse;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Task;
import com.walking.backend.repository.TaskRepository;
import com.walking.backend.repository.specification.TaskSpecification;
import com.walking.backend.service.TaskService;
import com.walking.backend.service.UserService;
import com.walking.backend.service.mapper.task.TaskRequestMapper;
import com.walking.backend.service.mapper.task.TaskResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final UserService userService;
    private final TaskRequestMapper taskRequestMapper;
    private final TaskResponseMapper taskResponseMapper;

    @Override
    public Page<TaskResponse> getTasks(Long userId, Boolean completed, Boolean today, Pageable pageable) {
        Specification<Task> spec = Specification.where(TaskSpecification.hasUserId(userId));

        if (completed != null) {
            spec = spec.and(TaskSpecification.isCompleted(completed));
        }
        if (Boolean.TRUE.equals(today)) {
            spec = spec.and(TaskSpecification.hasTodayFlag());
        }

        return taskRepository.findAll(spec, pageable)
                .map(taskResponseMapper::toDto);
    }

    @Override
    @Transactional
    public TaskResponse createTask(TaskRequest taskRequest, Long userId) {
        return Optional.of(taskRequest)
                .map(taskRequestMapper::toEntity)
                .map(task -> {
                    task.setIsCompleted(false);
                    task.setUser(userService.getUserById(userId));
                    return taskRepository.save(task);
                }).map(taskResponseMapper::toDto)
                .orElseThrow();
    }

    @Override
    @PreAuthorize("@userAccessChecker.isOwnerOfTask(#taskId, principal.id)")
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest taskRequest) {
        return taskRepository.findById(taskId)
                .map(task -> {
                    task.setTitle(taskRequest.title());
                    task.setDescription(taskRequest.description());
                    return taskRepository.save(task);
                })
                .map(taskResponseMapper::toDto)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id '%d' not found".formatted(taskId)));
    }

    @Override
    @PreAuthorize("@userAccessChecker.isOwnerOfTask(#taskId, principal.id)")
    @Transactional
    public TaskResponse toggleCompleted(Long taskId) {
        return taskRepository.findById(taskId)
                .map(task -> {
                    task.setIsCompleted(!task.getIsCompleted());
                    return taskRepository.save(task);
                })
                .map(taskResponseMapper::toDto)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id '%d' not found".formatted(taskId)));
    }

    @Override
    @PreAuthorize("@userAccessChecker.isOwnerOfTask(#taskId, principal.id)")
    @Transactional
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id '%d' not found".formatted(taskId)));

        taskRepository.delete(task);
    }
}
