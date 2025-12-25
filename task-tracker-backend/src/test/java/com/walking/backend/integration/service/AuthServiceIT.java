package com.walking.backend.integration.service;

import com.redis.testcontainers.RedisContainer;
import com.walking.backend.domain.dto.auth.AuthResponse;
import com.walking.backend.domain.dto.auth.SignInRequest;
import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.domain.exception.AuthException;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.service.AuthService;
import com.walking.backend.service.KafkaProducerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Sql(scripts = "classpath:sql/data.sql")
@Transactional
public class AuthServiceIT {
    private static final String USERNAME = "Zoxka";
    private static final String PASSWORD = "Zox617";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static final RedisContainer redis = new RedisContainer("redis:8-alpine");

    @MockitoBean
    private KafkaProducerService kafkaProducerService;

    @Value("${security.jwt.header.refresh-token}")
    private String refreshHeader;

    @Autowired
    private AuthService authService;

    @Test
    void signUp_whenValidRequestData_returnAuthResponse() {
        SignUpRequest signUpRequest = new SignUpRequest("Sveta", "sveta@gmail.com", "Sveta123");

        AuthResponse authResponse = authService.signUp(signUpRequest);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isNotBlank();
        assertThat(authResponse.refreshToken()).isNotBlank();

        verify(kafkaProducerService).sendMessageDto(anyString(), any(MessageDto.class));
    }

    @Test
    void signUp_whenUsernameAlreadyExists_throwDuplicateException() {
        SignUpRequest signUpRequest = new SignUpRequest(USERNAME, "sveta@gmail.com", "Sveta123");

        assertThatThrownBy(() -> authService.signUp(signUpRequest))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("This username '%s' is already taken".formatted(USERNAME));

        verifyNoInteractions(kafkaProducerService);
    }

    @Test
    void signIn_whenValidCredentials_returnAuthResponse() {
        SignInRequest signInRequest = new SignInRequest(USERNAME, PASSWORD);

        AuthResponse authResponse = authService.signIn(signInRequest);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isNotBlank();
        assertThat(authResponse.refreshToken()).isNotBlank();
    }

    @Test
    void signIn_whenInvalidCredentials_throwAuthException() {
        SignInRequest signInRequest = new SignInRequest(USERNAME, "InvalidPassword123");

        assertThatThrownBy(() -> authService.signIn(signInRequest))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void refreshTokens_whenValidRefreshToken_returnAuthResponse() {
        AuthResponse authResponse = authService.signIn(new SignInRequest(USERNAME, PASSWORD));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(refreshHeader, authResponse.refreshToken());

        AuthResponse refreshed = authService.refreshTokens(request);

        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotBlank();
        assertThat(refreshed.accessToken()).isNotEqualTo(authResponse.accessToken());
        assertThat(refreshed.refreshToken()).isNotEqualTo(authResponse.refreshToken());
    }

    @Test
    void refreshTokens_whenTokenMissing_throwAuthException() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> authService.refreshTokens(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Refresh token not transformed");
    }

    @Test
    void refreshTokens_whenTokenIsEmpty_throwAuthException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(refreshHeader, "");

        assertThatThrownBy(() -> authService.refreshTokens(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Refresh token not transformed");
    }

    @Test
    void refreshTokens_whenTokenInvalid_throwAuthException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(refreshHeader, "invalid.refresh.token");

        assertThatThrownBy(() -> authService.refreshTokens(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid or malformed token");
    }
}
