package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.activity.UserActivityResponse;
import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.security.CustomUserDetails;
import com.walking.backend.service.UserActivityService;
import com.walking.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserActivityService userActivityService;

    @GetMapping("/me")
    public UserResponse getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return userService.getUserByUsername(userDetails.username());
    }

    @GetMapping("/activities")
    public Page<UserActivityResponse> getUserActivities(
            @AuthenticationPrincipal CustomUserDetails userDetails, Pageable pageable) {
        return userActivityService.getUserActivities(userDetails.id(), pageable);
    }
}
