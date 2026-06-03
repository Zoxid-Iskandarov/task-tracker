package com.walking.backend.web.openapi;

import com.walking.backend.domain.dto.user.UpdateUserProfileRequest;
import com.walking.backend.domain.dto.user.UserProfileResponse;
import com.walking.backend.domain.dto.user.UserPublicProfileResponse;
import com.walking.backend.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User Profile", description = "Endpoints for managing user profiles and public information")
@SecurityRequirement(name = "Bearer Authentication")
public interface UserApi {

    @Operation(summary = "Get current user profile info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile data retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me")
    UserProfileResponse getMyProfile(@Parameter(hidden = true) CustomUserDetails userDetails);

    @Operation(summary = "Update user profile details", description = "Updates displayName and bio for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PatchMapping("/me")
    UserProfileResponse updateUserProfile(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @RequestBody UpdateUserProfileRequest request
    );

    @Operation(summary = "Upload user avatar", description = "Uploads an image file as a new avatar.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file type or size")
    })
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    UserProfileResponse uploadAvatar(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file
    );

    @Operation(summary = "Delete user avatar", description = "Removes the user's current avatar.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Avatar deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User profile not found")
    })
    @DeleteMapping("/me/avatar")
    ResponseEntity<Void> deleteAvatar(@Parameter(hidden = true) CustomUserDetails userDetails);

    @Operation(summary = "Get public user profile", description = "Retrieves public profile information by user ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile data retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserPublicProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    UserPublicProfileResponse getUserProfile(@PathVariable Long userId);
}
