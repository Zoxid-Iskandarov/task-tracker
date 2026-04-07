package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.auth.AuthResponse;
import com.walking.backend.domain.dto.auth.SignInRequest;
import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<AuthResponse> signUp(@RequestBody @Validated SignUpRequest signUpRequest,
                                               HttpServletResponse response) {
        AuthResponse authResponse = authService.signUp(signUpRequest, response);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authResponse);
    }

    @PostMapping("/sign-in")
    public AuthResponse signIn(@RequestBody @Validated SignInRequest signInRequest,
                               HttpServletResponse response) {
        return authService.signIn(signInRequest, response);
    }

    @PostMapping("/refresh")
    public AuthResponse refreshTokens(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                      HttpServletResponse response) {
        return authService.refreshToken(refreshToken, response);
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut() {
        // This method is intercepted by Spring Security Logout Filter
        return ResponseEntity.ok().build();
    }
}
