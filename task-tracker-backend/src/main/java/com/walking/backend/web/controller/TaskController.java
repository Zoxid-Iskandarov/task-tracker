package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.task.TaskRequest;
import com.walking.backend.domain.dto.task.TaskResponse;
import com.walking.backend.security.CustomUserDetails;
import com.walking.backend.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getUserTasks(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(taskService.getUserTasks(userDetails.id()));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@RequestBody @Validated TaskRequest createTaskRequest,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(taskService.createTask(createTaskRequest, userDetails.id()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable("id") Long taskId,
                                                   @RequestBody @Validated TaskRequest taskRequest) {
        return ResponseEntity.ok(taskService.updateTask(taskId, taskRequest));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponse> toggleCompleted(@PathVariable("id") Long taskId) {
        return ResponseEntity.ok(taskService.toggleCompleted(taskId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable("id") Long taskId) {
        taskService.deleteTask(taskId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
