package com.walking.backend.service;

import com.walking.backend.domain.dto.auth.AuthResponse;
import com.walking.backend.domain.dto.auth.SignInRequest;
import com.walking.backend.domain.dto.auth.SignUpRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    AuthResponse signUp(SignUpRequest signUpRequest, HttpServletResponse response);

    AuthResponse signIn(SignInRequest signInRequest, HttpServletResponse response);

    AuthResponse refreshToken(String refreshToken, HttpServletResponse response);
}
