package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.auth.AuthResponse;
import com.walking.backend.domain.dto.auth.SignInRequest;
import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.service.AuthService;
import com.walking.backend.web.openapi.AuthApi;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final AuthService authService;

    @Override
    @PostMapping("/sign-up")
    public ResponseEntity<AuthResponse> signUp(@RequestBody @Validated SignUpRequest signUpRequest) {
        AuthResponse authResponse = authService.singUp(signUpRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.AUTHORIZATION, authResponse.accessToken())
                .body(authResponse);
    }

    @Override
    @PostMapping("/sign-in")
    public ResponseEntity<AuthResponse> signIn(@RequestBody @Validated SignInRequest signInRequest) {
        AuthResponse authResponse = authService.signIn(signInRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, authResponse.accessToken())
                .body(authResponse);
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshTokens(HttpServletRequest request) {
        AuthResponse authResponse = authService.refreshTokens(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, authResponse.accessToken())
                .body(authResponse);
    }
}
