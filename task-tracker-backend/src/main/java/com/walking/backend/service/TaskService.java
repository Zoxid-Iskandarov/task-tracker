package com.walking.backend.service;

import com.walking.backend.domain.dto.task.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    Page<TaskPreviewResponse> getTasks(Long sectionId, Pageable pageable);

    Page<TaskPreviewResponse> searchTasks(Long boardId, TaskFilter taskFilter, Pageable pageable);

    TaskFullResponse getTaskById(Long taskId);

    TaskFullResponse createTask(CreateTaskRequest createTaskRequest);

    TaskFullResponse updateTask(UpdateTaskRequest updateTaskRequest, Long taskId);

    void deleteTask(Long taskId);

    TaskPreviewResponse toggleCompleted(Long taskId);

    TaskPreviewResponse moveTask(Long taskId, MoveTaskRequest moveTaskRequest);

    TaskPreviewResponse addLabelToTask(Long taskId, Long labelId);

    TaskPreviewResponse deleteLabelFromTask(Long taskId, Long labelId);
}
