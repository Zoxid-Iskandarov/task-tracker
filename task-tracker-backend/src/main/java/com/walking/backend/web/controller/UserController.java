package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.user.UpdateUserProfileRequest;
import com.walking.backend.domain.dto.user.UserProfileResponse;
import com.walking.backend.domain.dto.user.UserPublicProfileResponse;
import com.walking.backend.security.principal.CustomUserDetails;
import com.walking.backend.service.UserService;
import com.walking.backend.web.openapi.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements UserApi {
    private final UserService userService;

    @GetMapping("/me")
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return userService.getCurrentUserProfileById(userDetails.id());
    }

    @PatchMapping("/me")
    public UserProfileResponse updateUserProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Validated UpdateUserProfileRequest updateUserProfileRequest) {
        return userService.updateUserProfile(userDetails.id(), updateUserProfileRequest);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadAvatar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        userService.uploadAvatar(userDetails.id(), file);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.deleteAvatar(userDetails.id());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    public UserPublicProfileResponse getUserProfile(@PathVariable Long userId) {
        return userService.getUserProfileById(userId);
    }
}
