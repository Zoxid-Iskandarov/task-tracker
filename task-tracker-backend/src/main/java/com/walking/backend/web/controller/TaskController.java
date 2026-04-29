package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.task.*;
import com.walking.backend.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @GetMapping("/{taskId}")
    public TaskFullResponse getTaskById(@PathVariable Long taskId) {
        return taskService.getTaskById(taskId);
    }

    @PatchMapping("/{taskId}/toggle")
    public TaskPreviewResponse toggleCompleted(@PathVariable Long taskId) {
        return taskService.toggleCompleted(taskId);
    }

    @PatchMapping("/{taskId}/move")
    public TaskPreviewResponse moveTask(
            @PathVariable Long taskId,
            @RequestBody @Validated MoveTaskRequest moveTaskRequest) {
        return taskService.moveTask(taskId, moveTaskRequest);
    }

    @PostMapping("/{taskId}/labels/{labelId}")
    public TaskPreviewResponse addLabelToTask(@PathVariable Long taskId, @PathVariable Long labelId) {
        return taskService.addLabelToTask(taskId, labelId);
    }

    @DeleteMapping("/{taskId}/labels/{labelId}")
    public TaskPreviewResponse deleteLabelFromTask(@PathVariable Long taskId, @PathVariable Long labelId) {
        return taskService.deleteLabelFromTask(taskId, labelId);
    }

    @PostMapping
    public ResponseEntity<TaskFullResponse> createTask(@RequestBody @Validated CreateTaskRequest createTaskRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(createTaskRequest));
    }

    @PutMapping("/{taskId}")
    public TaskFullResponse updateTask(
            @RequestBody @Validated UpdateTaskRequest updateTaskRequest,
            @PathVariable Long taskId) {
        return taskService.updateTask(updateTaskRequest, taskId);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);

        return ResponseEntity.noContent().build();
    }
}
