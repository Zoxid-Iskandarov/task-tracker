package com.walking.backend.service;

import com.walking.backend.domain.dto.auth.AuthResponse;
import com.walking.backend.domain.dto.auth.SignInRequest;
import com.walking.backend.domain.dto.auth.SignUpRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthResponse singUp(SignUpRequest signUpRequest);

    AuthResponse signIn(SignInRequest signInRequest);

    AuthResponse refreshTokens(HttpServletRequest request);
}
