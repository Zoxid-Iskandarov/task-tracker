package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.auth.AuthResponse;
import com.walking.backend.domain.dto.auth.SignInRequest;
import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.domain.exception.AuthException;
import com.walking.backend.security.TokenService;
import com.walking.backend.service.AuthService;
import com.walking.backend.service.KafkaProducerService;
import com.walking.backend.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
    private final KafkaProducerService kafkaProducerService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse signUp(SignUpRequest signUpRequest, HttpServletResponse response) {
        UserResponse userResponse = userService.createUser(signUpRequest);
        kafkaProducerService.sendMessageDto(userResponse.id().toString(), createMessage(userResponse));

        return tokenService.generateTokens(userResponse.username(), userResponse.id(), response);
    }

    @Override
    public AuthResponse signIn(SignInRequest signInRequest, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    signInRequest.username(), signInRequest.password()));
        } catch (AuthenticationException e) {
            throw new AuthException("Invalid username or password");
        }

        UserResponse userResponse = userService.getUserByUsername(signInRequest.username());
        tokenService.deleteRefreshToken(userResponse.id());

        return tokenService.generateTokens(userResponse.username(), userResponse.id(), response);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken, HttpServletResponse response) {
        return tokenService.validateAndRefreshToken(refreshToken, response);
    }

    private MessageDto createMessage(UserResponse userResponse) {
        String message = """
                Hello, %s!
                
                Welcome to Task Tracker!
                Your registration was successful, and your account is now ready to use.
                
                Start creating and managing your tasks to stay organized and productive.
                
                Best regards,
                Task Tracker Team
                """.formatted(userResponse.username());

        return new MessageDto(userResponse.email(), "Welcome to Task Tracker", message);
    }
}
