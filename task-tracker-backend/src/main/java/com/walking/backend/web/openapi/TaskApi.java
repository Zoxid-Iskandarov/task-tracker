package com.walking.backend.web.openapi;

import com.walking.backend.domain.dto.attachment.TaskAttachmentDownloadResponse;
import com.walking.backend.domain.dto.attachment.TaskAttachmentResponse;
import com.walking.backend.domain.dto.task.*;
import com.walking.backend.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Tasks Management", description = "Endpoints for managing task lifecycles, states, positions, and label assignments")
@SecurityRequirement(name = "Bearer Authentication")
public interface TaskApi {

    @Operation(
            summary = "Get full details of a task",
            description = "Retrieves complete information about a specific task, including description, attachments, comments, and members."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TaskFullResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - No access to this board"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    TaskFullResponse getTaskById(
            @Parameter(description = "ID of the task to retrieve") Long taskId
    );

    @Operation(
            summary = "Toggle task completion status",
            description = "Quickly flips the 'completed' boolean status of a task (checks or unchecks it)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task completion toggled successfully",
                    content = @Content(schema = @Schema(implementation = TaskPreviewResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    TaskPreviewResponse toggleCompleted(
            @Parameter(description = "ID of the task to toggle") Long taskId
    );

    @Operation(
            summary = "Move task to another position or section",
            description = "Handles drag-and-drop operations, changing the parent column (section) and/or reordering the priority sequence inside the list."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task successfully moved",
                    content = @Content(schema = @Schema(implementation = TaskPreviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid section ID or bad position sequencing")
    })
    TaskPreviewResponse moveTask(
            @Parameter(description = "ID of the task to move") Long taskId,
            @RequestBody @Validated MoveTaskRequest moveTaskRequest
    );

    @Operation(
            summary = "Assign a label to a task",
            description = "Links an existing board label to a specified task for categorization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Label successfully added",
                    content = @Content(schema = @Schema(implementation = TaskPreviewResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task or Label not found")
    })
    TaskPreviewResponse addLabelToTask(
            @Parameter(description = "ID of the target task") Long taskId,
            @Parameter(description = "ID of the label to assign") Long labelId
    );

    @Operation(
            summary = "Remove a label from a task",
            description = "Detaches a label from the task without deleting the label itself from the system."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Label successfully removed",
                    content = @Content(schema = @Schema(implementation = TaskPreviewResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task or Label not found")
    })
    TaskPreviewResponse deleteLabelFromTask(
            @Parameter(description = "ID of the target task") Long taskId,
            @Parameter(description = "ID of the assigned label to remove") Long labelId
    );

    @Operation(
            summary = "Create a new task",
            description = "Creates a task inside a specified board and section column."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task successfully created",
                    content = @Content(schema = @Schema(implementation = TaskFullResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request attributes payload")
    })
    ResponseEntity<TaskFullResponse> createTask(
            @RequestBody @Validated CreateTaskRequest createTaskRequest
    );

    @Operation(
            summary = "Update task fields",
            description = "Modifies complete fields of a task such as its title, description, deadlines, and parameters."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task successfully updated",
                    content = @Content(schema = @Schema(implementation = TaskFullResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    TaskFullResponse updateTask(
            @RequestBody @Validated UpdateTaskRequest updateTaskRequest,
            @Parameter(description = "ID of the task to update") Long taskId
    );

    @Operation(
            summary = "Delete a task",
            description = "Permanently deletes a task from the board workspace."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task successfully deleted"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    ResponseEntity<?> deleteTask(
            @Parameter(description = "ID of the task to deleteAttachment") Long taskId
    );

    @Operation(
            summary = "Get all attachments for a task",
            description = "Retrieves a list of all files uploaded to the specified task."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of attachments retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskAttachmentResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Forbidden - No access to this task"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    List<TaskAttachmentResponse> getAttachments(
            @Parameter(description = "ID of the task") Long taskId
    );

    @Operation(
            summary = "Get download link for an attachment",
            description = "Generates a time-limited presigned URL to download a specific file."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Download URL generated successfully",
                    content = @Content(schema = @Schema(implementation = TaskAttachmentDownloadResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - No access to this task or attachment"),
            @ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    TaskAttachmentDownloadResponse getDownloadAttachment(
            @Parameter(description = "ID of the task") Long taskId,
            @Parameter(description = "ID of the attachment") Long attachmentId
    );

    @Operation(
            summary = "Upload a file to a task",
            description = "Uploads a new file as an attachment to the task. Respects file size and type limitations defined in system configuration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = TaskAttachmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or limit exceeded"),
            @ApiResponse(responseCode = "403", description = "Forbidden - No access to edit this task")
    })
    TaskAttachmentResponse uploadAttachment(
            @Parameter(description = "ID of the task") Long taskId,
            @Parameter(description = "File to upload") MultipartFile file,
            @Parameter(hidden = true) CustomUserDetails userDetails
    );

    @Operation(
            summary = "Delete an attachment",
            description = "Permanently removes a file attachment from the task and storage."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Attachment successfully deleted"),
            @ApiResponse(responseCode = "403", description = "Forbidden - No access to edit this task"),
            @ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    ResponseEntity<Void> deleteAttachment(
            @Parameter(description = "ID of the task") Long taskId,
            @Parameter(description = "ID of the attachment") Long attachmentId
    );
}
