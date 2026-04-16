package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.task.TaskRequest;
import com.walking.backend.domain.dto.task.TaskResponse;
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

    @PatchMapping("/{taskId}/toggle")
    public TaskResponse toggleCompleted(@PathVariable Long taskId) {
        return taskService.toggleCompleted(taskId);
    }

    @PatchMapping("/{taskId}/section/{sectionId}")
    public TaskResponse moveTask(@PathVariable Long taskId, @PathVariable Long sectionId) {
        return taskService.moveTask(taskId, sectionId);
    }

    @PostMapping("/{taskId}/labels/{labelId}")
    public TaskResponse addLabelToTask(@PathVariable Long taskId, @PathVariable Long labelId) {
        return taskService.addLabelToTask(taskId, labelId);
    }

    @DeleteMapping("/{taskId}/labels/{labelId}")
    public TaskResponse deleteLabelFromTask(@PathVariable Long taskId, @PathVariable Long labelId) {
        return taskService.deleteLabelFromTask(taskId, labelId);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@RequestBody @Validated TaskRequest taskRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(taskRequest));
    }

    @PutMapping("/{taskId}")
    public TaskResponse updateTask(@RequestBody @Validated TaskRequest taskRequest, @PathVariable Long taskId) {
        return taskService.updateTask(taskRequest, taskId);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);

        return ResponseEntity.noContent().build();
    }
}
