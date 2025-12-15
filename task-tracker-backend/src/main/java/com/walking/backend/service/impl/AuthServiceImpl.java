package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.auth.AuthResponse;
import com.walking.backend.domain.dto.auth.SignInRequest;
import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.domain.exception.AuthException;
import com.walking.backend.security.JwtService;
import com.walking.backend.security.TokenService;
import com.walking.backend.service.AuthService;
import com.walking.backend.service.KafkaProducerService;
import com.walking.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final KafkaProducerService kafkaProducerService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse singUp(SignUpRequest signUpRequest) {
        UserResponse userResponse = userService.createUser(signUpRequest);
        kafkaProducerService.sendMessageDto(userResponse.id().toString(), createMessage(userResponse));

        return generateTokens(userResponse);
    }

    @Override
    public AuthResponse signIn(SignInRequest signInRequest) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    signInRequest.username(), signInRequest.password()));
        } catch (AuthenticationException e) {
            throw new AuthException("Invalid username or password");
        }

        UserResponse userResponse = userService.getUserByUsername(signInRequest.username());
        tokenService.deleteAllTokensOfUser(userResponse.id());

        return generateTokens(userResponse);
    }

    @Override
    public AuthResponse refreshTokens(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthException("Refresh token not transformed");
        }

        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        UserResponse userResponse = userService.getUserByUsername(username);

        if (!jwtService.isValidRefreshToken(token, userResponse.username(), userResponse.id())) {
            throw new AuthException("Refresh token is revoked");
        }

        tokenService.deleteAllTokensOfUser(userResponse.id());

        return generateTokens(userResponse);
    }

    private AuthResponse generateTokens(UserResponse userResponse) {
        String accessToken = jwtService.generateAccessToken(userResponse.username());
        String refreshToken = jwtService.generateRefreshToken(userResponse.username());

        tokenService.saveAccessToken(accessToken, userResponse.id());
        tokenService.saveRefreshToken(refreshToken, userResponse.id());

        return new AuthResponse(accessToken, refreshToken);
    }

    private MessageDto createMessage(UserResponse userResponse) {
        String message = """
                Hello, %s!
                We're glad to welcome you among our users. Registration completed successfully.
                """.formatted(userResponse.username());

        return new MessageDto(userResponse.email(), "Welcome to Task Tracker", message);
    }
}
