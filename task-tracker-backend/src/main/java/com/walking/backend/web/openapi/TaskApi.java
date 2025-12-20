package com.walking.backend.web.openapi;

import com.walking.backend.domain.dto.task.TaskRequest;
import com.walking.backend.domain.dto.task.TaskResponse;
import com.walking.backend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "Tasks", description = "Operations related to task management")
public interface TaskApi {

    @Operation(
            summary = "Get all tasks for current user",
            description = "Returns a paginated list of tasks. Allows filtering by completion status and date."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of tasks retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<Page<TaskResponse>> getTasks(
            CustomUserDetails userDetails,
            @Parameter(description = "Filter by task completion status") Boolean completed,
            @Parameter(description = "Filter to get only today's tasks") Boolean today,
            @ParameterObject Pageable pageable
    );

    @Operation(summary = "Create a new task", description = "Saves a new task and links it to the authenticated user.")
    @ApiResponse(responseCode = "201", description = "Task created successfully")
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<TaskResponse> createTask(TaskRequest createTaskRequest, CustomUserDetails userDetails);

    @Operation(summary = "Update task details", description = "Updates title and description. Ownership check is performed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task updated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - You do not own this task"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<TaskResponse> updateTask(Long taskId, TaskRequest taskRequest);

    @Operation(summary = "Toggle task completion status", description = "Inverts the 'isCompleted' flag of the task.")
    @ApiResponse(responseCode = "200", description = "Status toggled successfully")
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<TaskResponse> toggleCompleted(Long taskId);

    @Operation(summary = "Delete task", description = "Permanently removes a task from the system.")
    @ApiResponse(responseCode = "204", description = "Task deleted successfully")
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<Void> deleteTask(Long taskId);
}
