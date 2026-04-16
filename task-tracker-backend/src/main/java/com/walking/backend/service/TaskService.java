package com.walking.backend.service;

import com.walking.backend.domain.dto.task.TaskFilter;
import com.walking.backend.domain.dto.task.TaskRequest;
import com.walking.backend.domain.dto.task.TaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    Page<TaskResponse> getTasks(Long sectionId, Pageable pageable);

    Page<TaskResponse> searchTasks(Long boardId, TaskFilter taskFilter, Pageable pageable);

    TaskResponse createTask(TaskRequest taskRequest);

    TaskResponse updateTask(TaskRequest taskRequest, Long taskId);

    void deleteTask(Long taskId);

    TaskResponse toggleCompleted(Long taskId);

    TaskResponse moveTask(Long taskId, Long sectionId);

    TaskResponse addLabelToTask(Long taskId, Long labelId);

    TaskResponse deleteLabelFromTask(Long taskId, Long labelId);
}
