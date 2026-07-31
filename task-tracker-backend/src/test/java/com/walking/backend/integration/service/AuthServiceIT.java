package com.walking.backend.integration.service;

import com.walking.backend.domain.dto.auth.AuthResponse;
import com.walking.backend.domain.dto.auth.SignInRequest;
import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.domain.exception.AuthException;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.model.User;
import com.walking.backend.domain.model.UserProfile;
import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.props.AppProperties;
import com.walking.backend.repository.UserProfileRepository;
import com.walking.backend.repository.UserRepository;
import com.walking.backend.security.authentication.JwtService;
import com.walking.backend.security.authentication.TokenService;
import com.walking.backend.service.KafkaProducerService;
import com.walking.backend.service.impl.AuthServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RequiredArgsConstructor
public class AuthServiceIT extends IntegrationTestBase {
    private final AuthServiceImpl authService;
    private final KafkaProducerService kafkaProducerService;
    private final TokenService tokenService;
    private final JwtService jwtService;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    @Test
    void signUp_whenValidRequestData_shouldReturnAuthResponse() {
        doNothing().when(kafkaProducerService).sendMessageDto(anyLong(), any(MessageDto.class));

        var mockResponse = new MockHttpServletResponse();
        var signUpRequest = new SignUpRequest("dante", "dante@gmail.com", "Dante123");

        AuthResponse actual = authService.signUp(signUpRequest, mockResponse);

        assertNotNull(actual);
        assertThat(actual.accessToken()).isNotBlank();

        Optional<User> optionalUser = userRepository.findByUsername("dante");
        assertThat(optionalUser).isPresent();

        User savedUser = optionalUser.orElseThrow();

        Optional<UserProfile> userProfile = userProfileRepository.findById(savedUser.getId());
        assertThat(userProfile).isPresent();

        verify(kafkaProducerService).sendMessageDto(anyLong(), any(MessageDto.class));
        verify(tokenService).generateTokens(savedUser.getUsername(), savedUser.getId(), mockResponse);
    }

    @Test
    void signUp_whenUserServiceThrowException_shouldNotCallKafkaAndTokenService() {
        var signUpRequest = new SignUpRequest("john_doe", "john.doe@example.com", "password123");
        var mockResponse = new MockHttpServletResponse();

        assertThatThrownBy(() -> authService.signUp(signUpRequest, mockResponse))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("Username %s is already taken".formatted(signUpRequest.username()));

        verifyNoInteractions(kafkaProducerService, tokenService);
    }

    @Test
    void signUp_whenKafkaProducerServiceThrowException_shouldNotCallTokenService() {
        doThrow(RuntimeException.class).when(kafkaProducerService).sendMessageDto(anyLong(), any(MessageDto.class));

        var signUpRequest = new SignUpRequest("dante", "dante@gmail.com", "Dante123");
        var mockResponse = new MockHttpServletResponse();

        assertThatThrownBy(() -> authService.signUp(signUpRequest, mockResponse))
                .isInstanceOf(RuntimeException.class);

        verify(kafkaProducerService).sendMessageDto(anyLong(), any(MessageDto.class));
        verifyNoInteractions(tokenService);
    }

    @Test
    void signIn_whenValidRequestData_shouldReturnAuthResponse() {
        var signInRequest = new SignInRequest("john_doe", "password123");
        var mockResponse = new MockHttpServletResponse();

        AuthResponse actual = authService.signIn(signInRequest, mockResponse);

        assertThat(actual).isNotNull();
        assertThat(actual.accessToken()).isNotBlank();

        verify(tokenService).deleteRefreshToken(anyLong());
        verify(tokenService).generateTokens(eq(signInRequest.username()), anyLong(), any(HttpServletResponse.class));
    }

    @Test
    void signIn_whenInvalidRequestData_shouldThrowAuthException() {
        var signInRequest = new SignInRequest("john_doe", "incorrect_password123");
        var mockResponse = new MockHttpServletResponse();

        assertThatThrownBy(() -> authService.signIn(signInRequest, mockResponse))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid username or password");

        verifyNoInteractions(tokenService);
    }

    @Test
    void refreshToken_whenRefreshTokenIsNull_shouldThrowAuthException() {
        var mockResponse = new MockHttpServletResponse();

        assertThatThrownBy(() -> authService.refreshToken(null, mockResponse))
                .isInstanceOf(AuthException.class)
                .hasMessage("Refresh token not passed");
    }

    @Test
    void refreshToken_whenTokenNotFoundInRedis_shouldThrowAuthException() {
        var mockResponse = new MockHttpServletResponse();
        String fakeToken = jwtService.generateRefreshToken("dante");

        assertThatThrownBy(() -> authService.refreshToken(fakeToken, mockResponse))
                .isInstanceOf(AuthException.class)
                .hasMessage("Refresh token is revoked");
    }

    @Test
    void refreshToken_whenValidToken_shouldReturnNewTokens() {
        var mockSignInResponse = new MockHttpServletResponse();
        authService.signIn(new SignInRequest("john_doe", "password123"), mockSignInResponse);

        String setCookieHeader = mockSignInResponse.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).isNotBlank();

        assert setCookieHeader != null;
        String refreshToken = extractTokenFromCookie(setCookieHeader, appProperties.getSecurity().getJwt().getCookieName());

        var mockRefreshResponse = new MockHttpServletResponse();
        AuthResponse actual = authService.refreshToken(refreshToken, mockRefreshResponse);

        assertThat(actual).isNotNull();
        assertThat(actual.accessToken()).isNotBlank();

        String oldTokenKey = appProperties.getSecurity().getJwt().getRedis().getRefreshTokenPrefix() + refreshToken;
        assertThat(redisTemplate.hasKey(oldTokenKey)).isFalse();

        String newSetCookieHeader = mockRefreshResponse.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(newSetCookieHeader).isNotBlank();
    }

    private String extractTokenFromCookie(String setCookieHeader, String cookieName) {
        return Arrays.stream(setCookieHeader.split(";"))
                .map(String::trim)
                .filter(part -> part.startsWith("%s=".formatted(cookieName)))
                .map(part -> part.substring(cookieName.length() + 1))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cookie %s not found in Set-Cookie header".formatted(cookieName)));
    }
}
