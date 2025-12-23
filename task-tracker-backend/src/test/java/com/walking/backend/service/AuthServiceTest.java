package com.walking.backend.service;

import com.walking.backend.domain.dto.auth.AuthResponse;
import com.walking.backend.domain.dto.auth.SignInRequest;
import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.domain.exception.AuthException;
import com.walking.backend.security.JwtService;
import com.walking.backend.security.TokenService;
import com.walking.backend.service.impl.AuthServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    private static final Long ID = 1L;
    private static final String USERNAME = "Test";
    private static final String EMAIL = "test@gmail.com";
    private static final String PASSWORD = "Password123";
    private static final String ACCESS_TOKEN = "access_1aBcDeFgHiJkLmNoPqRsTuVwXyZ_abcdef1234567890";
    private static final String REFRESH_TOKEN = "refresh_1aBcDeFgHiJkLmNoPqRsTuVwXyZ_abcdef1234567890";
    private static final String REFRESH_HEADER = "X-Refresh-Token";

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

    @Mock
    private JwtService jwtService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshHeader", REFRESH_HEADER);
    }

    @Test
    void signUp_whenValidRequestData_returnAuthResponse() {
        SignUpRequest signUpRequest = getSignUpRequest();
        UserResponse userResponse = getUserResponse();

        doReturn(userResponse).when(userService).createUser(signUpRequest);
        doReturn(ACCESS_TOKEN).when(jwtService).generateAccessToken(userResponse.username());
        doReturn(REFRESH_TOKEN).when(jwtService).generateRefreshToken(userResponse.username());

        AuthResponse actual = authService.signUp(signUpRequest);

        assertEquals(ACCESS_TOKEN, actual.accessToken());
        assertEquals(REFRESH_TOKEN, actual.refreshToken());

        verify(userService).createUser(signUpRequest);
        verify(kafkaProducerService).sendMessageDto(anyString(), any(MessageDto.class));
        verify(jwtService).generateAccessToken(userResponse.username());
        verify(jwtService).generateRefreshToken(userResponse.username());
        verify(tokenService).saveAccessToken(ACCESS_TOKEN, userResponse.id());
        verify(tokenService).saveRefreshToken(REFRESH_TOKEN, userResponse.id());

        verifyNoMoreInteractions(userService, kafkaProducerService, jwtService, tokenService);
    }

    @Test
    void signIn_whenValidCredentials_returnAuthResponse() {
        SignInRequest signInRequest = getSignInRequest();
        UserResponse userResponse = getUserResponse();

        doReturn(mock(Authentication.class)).when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        doReturn(userResponse).when(userService).getUserByUsername(signInRequest.username());
        doReturn(ACCESS_TOKEN).when(jwtService).generateAccessToken(userResponse.username());
        doReturn(REFRESH_TOKEN).when(jwtService).generateRefreshToken(userResponse.username());

        AuthResponse actual = authService.signIn(signInRequest);

        assertEquals(ACCESS_TOKEN, actual.accessToken());
        assertEquals(REFRESH_TOKEN, actual.refreshToken());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService).getUserByUsername(signInRequest.username());
        verify(tokenService).deleteAllTokensOfUser(userResponse.id());
        verify(jwtService).generateAccessToken(userResponse.username());
        verify(jwtService).generateRefreshToken(userResponse.username());
        verify(tokenService).saveAccessToken(ACCESS_TOKEN, userResponse.id());
        verify(tokenService).saveRefreshToken(REFRESH_TOKEN, userResponse.id());

        verifyNoMoreInteractions(authenticationManager, userService, tokenService, jwtService);
    }

    @Test
    void signIn_whenInvalidCredentials_throwAuthException() {
        SignInRequest signInRequest = new SignInRequest(USERNAME, PASSWORD);

        doThrow(BadCredentialsException.class).when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(AuthException.class, () -> authService.signIn(signInRequest));

        verifyNoMoreInteractions(authenticationManager);
        verifyNoInteractions(userService, tokenService, jwtService);
    }

    @Test
    void refreshTokens_whenValidRefreshToken_returnAuthResponse() {
        UserResponse userResponse = getUserResponse();

        doReturn(REFRESH_TOKEN).when(request).getHeader(REFRESH_HEADER);
        doReturn(USERNAME).when(jwtService).extractUsername(REFRESH_TOKEN);
        doReturn(userResponse).when(userService).getUserByUsername(USERNAME);
        doReturn(true).when(jwtService)
                .isValidRefreshToken(REFRESH_TOKEN, userResponse.username(), userResponse.id());
        doReturn(ACCESS_TOKEN).when(jwtService).generateAccessToken(userResponse.username());
        doReturn(REFRESH_TOKEN).when(jwtService).generateRefreshToken(userResponse.username());

        AuthResponse actual = authService.refreshTokens(request);

        assertEquals(ACCESS_TOKEN, actual.accessToken());
        assertEquals(REFRESH_TOKEN, actual.refreshToken());

        verify(request).getHeader(REFRESH_HEADER);
        verify(jwtService).extractUsername(REFRESH_TOKEN);
        verify(userService).getUserByUsername(USERNAME);
        verify(jwtService).isValidRefreshToken(REFRESH_TOKEN, userResponse.username(), userResponse.id());
        verify(tokenService).deleteAllTokensOfUser(userResponse.id());
        verify(jwtService).generateAccessToken(userResponse.username());
        verify(jwtService).generateRefreshToken(userResponse.username());
        verify(tokenService).saveAccessToken(ACCESS_TOKEN, userResponse.id());
        verify(tokenService).saveRefreshToken(REFRESH_TOKEN, userResponse.id());

        verifyNoMoreInteractions(request, jwtService, userService, tokenService);
    }

    @Test
    void refreshTokens_whenRefreshTokenMissing_throwAuthException() {
        doReturn(null).when(request).getHeader(REFRESH_HEADER);

        assertThrows(AuthException.class, () -> authService.refreshTokens(request));

        verify(request).getHeader(REFRESH_HEADER);
        verifyNoInteractions(jwtService, userService, tokenService);
    }

    @Test
    void refreshTokens_whenRefreshTokenInvalid_throwAuthException() {
        UserResponse userResponse = getUserResponse();

        doReturn(REFRESH_TOKEN).when(request).getHeader(REFRESH_HEADER);
        doReturn(USERNAME).when(jwtService).extractUsername(REFRESH_TOKEN);
        doReturn(userResponse).when(userService).getUserByUsername(USERNAME);
        doReturn(false).when(jwtService)
                .isValidRefreshToken(REFRESH_TOKEN, userResponse.username(), userResponse.id());

        assertThrows(AuthException.class, () -> authService.refreshTokens(request));

        verify(request).getHeader(REFRESH_HEADER);
        verify(jwtService).extractUsername(REFRESH_TOKEN);
        verify(userService).getUserByUsername(USERNAME);
        verify(jwtService).isValidRefreshToken(REFRESH_TOKEN, userResponse.username(), userResponse.id());
        verify(jwtService, never()).generateAccessToken(USERNAME);
        verify(jwtService, never()).generateRefreshToken(USERNAME);
        verifyNoInteractions(tokenService);
    }

    private SignUpRequest getSignUpRequest() {
        return new SignUpRequest(USERNAME, EMAIL, PASSWORD);
    }

    private SignInRequest getSignInRequest() {
        return new SignInRequest(USERNAME, PASSWORD);
    }

    private UserResponse getUserResponse() {
        return new UserResponse(ID, USERNAME, EMAIL);
    }
}
