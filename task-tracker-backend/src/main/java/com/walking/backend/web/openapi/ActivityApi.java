package com.walking.backend.web.openapi;

import com.walking.backend.domain.dto.activity.BoardActivityResponse;
import com.walking.backend.domain.dto.activity.UserActivityResponse;
import com.walking.backend.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Activity", description = "Endpoints for retrieving activity logs and audit trails")
@SecurityRequirement(name = "Bearer Authentication")
public interface ActivityApi {

    @Operation(
            summary = "Get paginated user activity history",
            description = "Retrieves a paginated log of all actions performed by the currently authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activity log retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserActivityResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/users/me/activities")
    Page<UserActivityResponse> getUserActivities(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject Pageable pageable
    );

    @Operation(
            summary = "Get historical board activity logs",
            description = "Retrieves audit trails and history logs showing actions performed on a specific board."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activity log retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BoardActivityResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to this board")
    })
    @GetMapping("/boards/{boardId}/activities")
    Page<BoardActivityResponse> getBoardActivities(
            @Parameter(description = "ID of the board") @PathVariable Long boardId,
            @ParameterObject Pageable pageable
    );
}
