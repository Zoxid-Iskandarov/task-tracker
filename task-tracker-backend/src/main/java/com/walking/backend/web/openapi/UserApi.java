package com.walking.backend.web.openapi;

import com.walking.backend.domain.dto.user.UserProfileResponse;
import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "User Profile", description = "Endpoints for managing user accounts and retrieving personal activity history")
@SecurityRequirement(name = "Bearer Authentication")
public interface UserApi {

    @Operation(
            summary = "Get current user profile info",
            description = "Retrieves information about the currently authenticated user based on the JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile data retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is invalid or missing")
    })
    UserProfileResponse getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
