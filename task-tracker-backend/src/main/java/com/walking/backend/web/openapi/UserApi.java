package com.walking.backend.web.openapi;

import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "User", description = "User Profile Management")
public interface UserApi {

    @Operation(
            summary = "Get information about the current user",
            description = "Returns profile data (id, username, email) based on a JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data received successfully"),
            @ApiResponse(responseCode = "401", description = "User is not authorized (missing or invalid token)"),
            @ApiResponse(responseCode = "404", description = "The user was not found in the database.")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<UserResponse> getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails);
}
