package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.task.TaskRequest;
import com.walking.backend.domain.dto.task.TaskResponse;
import com.walking.backend.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @GetMapping("/{sectionId}")
    public Page<TaskResponse> getTasks(@PathVariable Long sectionId, @PageableDefault(50) Pageable pageable) {
        return taskService.getTasks(sectionId, pageable);
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

    @PatchMapping("/{taskId}")
    public TaskResponse toggleCompleted(@PathVariable Long taskId) {
        return taskService.toggleCompleted(taskId);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);

        return ResponseEntity.noContent().build();
    }
}
