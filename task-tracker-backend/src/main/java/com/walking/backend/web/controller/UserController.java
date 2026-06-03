package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.user.UpdateUserProfileRequest;
import com.walking.backend.domain.dto.user.UserProfileResponse;
import com.walking.backend.domain.dto.user.UserPublicProfileResponse;
import com.walking.backend.security.principal.CustomUserDetails;
import com.walking.backend.service.UserService;
import com.walking.backend.web.openapi.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping("/{userId}")
    public UserPublicProfileResponse getUserProfile(@PathVariable Long userId) {
        return userService.getUserProfileById(userId);
    }
}
