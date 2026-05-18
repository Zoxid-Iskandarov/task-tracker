package com.walking.backend.web.openapi;

import com.walking.backend.domain.dto.label.CreateLabelRequest;
import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.dto.label.UpdateLabelRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;

@Tag(name = "Labels Management", description = "Endpoints for creating, updating, and deleting task tags/labels")
@SecurityRequirement(name = "Bearer Authentication")
public interface LabelApi {

    @Operation(
            summary = "Create a new label",
            description = "Creates a reusable tag with a specific name and color for tasks within a board context."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Label successfully created",
                    content = @Content(schema = @Schema(implementation = LabelResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid label metadata or name validation failure"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient board member permissions")
    })
    ResponseEntity<LabelResponse> createLabel(
            @RequestBody @Validated CreateLabelRequest createLabelRequest
    );

    @Operation(
            summary = "Update an existing label",
            description = "Modifies the title, style, or color configuration of a specific label by its ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Label successfully updated",
                    content = @Content(schema = @Schema(implementation = LabelResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User cannot modify elements on this board"),
            @ApiResponse(responseCode = "404", description = "Label not found")
    })
    LabelResponse updateLabel(
            @RequestBody @Validated UpdateLabelRequest updateLabelRequest,
            @Parameter(description = "ID of the label to update") Long labelId
    );

    @Operation(
            summary = "Delete a label",
            description = "Permanently deletes a label. Automatically detaches this label from all tasks where it was applied."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Label successfully deleted"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only workspace editors or owners can delete labels"),
            @ApiResponse(responseCode = "404", description = "Label not found")
    })
    ResponseEntity<?> deleteLabel(
            @Parameter(description = "ID of the label to delete") Long labelId
    );
}
