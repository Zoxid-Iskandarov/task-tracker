package com.walking.backend.service;

import com.walking.backend.domain.dto.task.TaskRequest;
import com.walking.backend.domain.dto.task.TaskResponse;

import java.util.List;

public interface TaskService {

    List<TaskResponse> getUserTasks(Long userId);

    TaskResponse createTask(TaskRequest taskRequest, Long userId);

    TaskResponse updateTask(Long taskId, TaskRequest taskRequest);

    TaskResponse toggleCompleted(Long taskId);

    void deleteTask(Long taskId);
}
