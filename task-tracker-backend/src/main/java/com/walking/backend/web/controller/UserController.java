package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.security.CustomUserDetails;
import com.walking.backend.service.UserService;
import com.walking.backend.web.openapi.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController implements UserApi {
    private final UserService userService;

    @Override
    @GetMapping
    public ResponseEntity<UserResponse> getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getUserByUsername(userDetails.username()));
    }
}
