package com.walking.backend.web.controller;

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

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements UserApi {
    private final UserService userService;

    @GetMapping("/me")
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return userService.getCurrentUserProfileById(userDetails.id());
    }

    @GetMapping("/{userId}")
    public UserPublicProfileResponse getUserProfile(@PathVariable Long userId) {
        return userService.getUserProfileById(userId);
    }
}
