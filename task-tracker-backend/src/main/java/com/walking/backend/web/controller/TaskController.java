package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.task.TaskRequest;
import com.walking.backend.domain.dto.task.TaskResponse;
import com.walking.backend.domain.model.Task_;
import com.walking.backend.security.CustomUserDetails;
import com.walking.backend.service.TaskService;
import com.walking.backend.web.openapi.TaskApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController implements TaskApi {
    private final TaskService taskService;

    @Override
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Boolean today,
            @PageableDefault(sort = Task_.CREATED, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(taskService.getTasks(userDetails.id(), completed, today, pageable));
    }

    @Override
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@RequestBody @Validated TaskRequest createTaskRequest,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(createTaskRequest, userDetails.id()));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable("id") Long taskId,
                                                   @RequestBody @Validated TaskRequest taskRequest) {
        return ResponseEntity.ok(taskService.updateTask(taskId, taskRequest));
    }

    @Override
    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponse> toggleCompleted(@PathVariable("id") Long taskId) {
        return ResponseEntity.ok(taskService.toggleCompleted(taskId));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable("id") Long taskId) {
        taskService.deleteTask(taskId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
