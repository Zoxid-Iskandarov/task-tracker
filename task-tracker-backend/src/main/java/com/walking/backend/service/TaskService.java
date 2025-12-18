package com.walking.backend.service;

import com.walking.backend.domain.dto.task.TaskRequest;
import com.walking.backend.domain.dto.task.TaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    Page<TaskResponse> getTasks(Long userId, Boolean completed, Boolean today, Pageable pageable);

    TaskResponse createTask(TaskRequest taskRequest, Long userId);

    TaskResponse updateTask(Long taskId, TaskRequest taskRequest);

    TaskResponse toggleCompleted(Long taskId);

    void deleteTask(Long taskId);
}
