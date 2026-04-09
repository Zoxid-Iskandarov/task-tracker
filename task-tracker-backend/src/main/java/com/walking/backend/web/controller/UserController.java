package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.security.CustomUserDetails;
import com.walking.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public UserResponse getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return userService.getUserByUsername(userDetails.username());
    }
}
