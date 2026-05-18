package com.walking.backend.web.openapi;

import com.walking.backend.domain.dto.section.CreateSectionRequest;
import com.walking.backend.domain.dto.section.SectionResponse;
import com.walking.backend.domain.dto.section.UpdateSectionRequest;
import com.walking.backend.domain.dto.task.TaskPreviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;

@Tag(name = "Sections Management", description = "Endpoints for managing board columns (sections) and fetching structured tasks")
@SecurityRequirement(name = "Bearer Authentication")
public interface SectionApi {

    @Operation(
            summary = "Get tasks inside a section",
            description = "Retrieves a paginated list of tasks assigned to a specific section (column), sorted by their inner position default."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - No access to the parent board"),
            @ApiResponse(responseCode = "404", description = "Section not found")
    })
    Page<TaskPreviewResponse> getTasks(
            @Parameter(description = "ID of the section/column") Long sectionId,
            @ParameterObject Pageable pageable
    );

    @Operation(
            summary = "Create a new section",
            description = "Adds a new vertical column to a board (e.g., 'To Do', 'In Progress', 'Done')."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Section successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid section metadata input")
    })
    ResponseEntity<?> createSection(
            @RequestBody @Validated CreateSectionRequest createSectionRequest
    );

    @Operation(
            summary = "Update section title or attributes",
            description = "Renames or changes the state/position rules of an existing board section."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Section updated successfully",
                    content = @Content(schema = @Schema(implementation = SectionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Section not found")
    })
    SectionResponse updateSection(
            @RequestBody @Validated UpdateSectionRequest updateSectionRequest,
            @Parameter(description = "ID of the section to update") Long sectionId
    );

    @Operation(
            summary = "Delete a section",
            description = "Permanently deletes a column. Depending on the business rules, this cascadingly deletes or clears tasks inside it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Section successfully removed"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    ResponseEntity<?> deleteSection(
            @Parameter(description = "ID of the section to delete") Long sectionId
    );
}
