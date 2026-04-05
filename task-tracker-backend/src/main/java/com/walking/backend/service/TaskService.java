package com.walking.backend.service;

import com.walking.backend.domain.dto.task.TaskRequest;
import com.walking.backend.domain.dto.task.TaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    Page<TaskResponse> getTasks(Long sectionId, Pageable pageable);

    TaskResponse createTask(TaskRequest taskRequest);

    TaskResponse updateTask(TaskRequest taskRequest, Long taskId);

    void deleteTask(Long taskId);

    TaskResponse toggleCompleted(Long taskId);
}
