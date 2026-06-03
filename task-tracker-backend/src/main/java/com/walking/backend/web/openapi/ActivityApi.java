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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

public interface ActivityApi {

    @Operation(
            summary = "Get paginated user activity history",
            description = "Retrieves a paginated log of all actions performed by the current user across all tracking boards."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activity log retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is invalid or missing")
    })
    Page<UserActivityResponse> getUserActivities(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject Pageable pageable
    );

    @Operation(summary = "Get historical board activity logs", description = "Retrieves audit trails and history logs showing what actions were performed on this specific board today and recently.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Activity log retrieved successfully")
    })
    Page<BoardActivityResponse> getBoardActivities(
            @Parameter(description = "ID of the board") Long boardId,
            @ParameterObject Pageable pageable
    );
}
