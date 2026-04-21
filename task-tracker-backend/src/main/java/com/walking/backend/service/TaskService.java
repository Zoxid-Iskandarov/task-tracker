package com.walking.backend.service;

import com.walking.backend.domain.dto.task.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    Page<TaskResponse> getTasks(Long sectionId, Pageable pageable);

    Page<TaskResponse> searchTasks(Long boardId, TaskFilter taskFilter, Pageable pageable);

    TaskResponse createTask(CreateTaskRequest createTaskRequest);

    TaskResponse updateTask(UpdateTaskRequest updateTaskRequest, Long taskId);

    void deleteTask(Long taskId);

    TaskResponse toggleCompleted(Long taskId);

    TaskResponse moveTask(Long taskId, MoveTaskRequest moveTaskRequest);

    TaskResponse addLabelToTask(Long taskId, Long labelId);

    TaskResponse deleteLabelFromTask(Long taskId, Long labelId);
}
