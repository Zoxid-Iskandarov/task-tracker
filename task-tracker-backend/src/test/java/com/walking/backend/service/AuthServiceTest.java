package com.walking.backend.service;

import com.walking.backend.domain.dto.auth.AuthResponse;
import com.walking.backend.domain.dto.auth.SignInRequest;
import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.domain.exception.AuthException;
import com.walking.backend.security.authentication.TokenService;
import com.walking.backend.security.principal.CustomUserDetails;
import com.walking.backend.service.impl.AuthServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    private static final Long ID = 1L;
    private static final String USERNAME = "Test";
    private static final String EMAIL = "test@gmail.com";
    private static final String PASSWORD = "Password123";
    private static final String ACCESS_TOKEN = "access_1aBcDeFgHiJkLmNoPqRsTuVwXyZ_abcdef1234567890";

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

    @Mock
    private HttpServletResponse response;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void singUp_whenValidRequestData_shouldReturnAuthResponse() {
        SignUpRequest signUpRequest = getSignUpRequest();
        UserResponse userResponse = getUserResponse();
        AuthResponse authResponse = getAuthResponse();

        doReturn(userResponse).when(userService).createUser(signUpRequest);
        doNothing().when(kafkaProducerService).sendMessageDto(anyLong(), any(MessageDto.class));
        doReturn(authResponse).when(tokenService).generateTokens(userResponse.username(), userResponse.id(), response);

        AuthResponse actual = authService.signUp(signUpRequest, response);

        Assertions.assertEquals(authResponse, actual);
        Assertions.assertEquals(authResponse.accessToken(), actual.accessToken());

        verify(userService).createUser(signUpRequest);
        verify(kafkaProducerService).sendMessageDto(anyLong(), any(MessageDto.class));
        verify(tokenService).generateTokens(userResponse.username(), userResponse.id(), response);
    }

    @Test
    void signUp_whenUserServiceThrowException_shouldNotCallKafkaAndTokenService() {
        doThrow(RuntimeException.class).when(userService).createUser(any(SignUpRequest.class));

        assertThrows(RuntimeException.class, () -> authService.signUp(getSignUpRequest(), response));

        verify(userService).createUser(any(SignUpRequest.class));
        verify(kafkaProducerService, never()).sendMessageDto(anyLong(), any(MessageDto.class));
        verify(tokenService, never()).generateTokens(anyString(), anyLong(), any());
    }

    @Test
    void signUp_whenKafkaProducerServiceThrowException_shouldNotCallTokenService() {
        SignUpRequest signUpRequest = getSignUpRequest();
        UserResponse userResponse = getUserResponse();

        doReturn(userResponse).when(userService).createUser(signUpRequest);
        doThrow(RuntimeException.class).when(kafkaProducerService).sendMessageDto(anyLong(), any(MessageDto.class));

        assertThrows(RuntimeException.class, () -> authService.signUp(signUpRequest, response));

        verify(userService).createUser(signUpRequest);
        verify(kafkaProducerService).sendMessageDto(anyLong(), any(MessageDto.class));
        verify(tokenService, never()).generateTokens(anyString(), anyLong(), any());
    }

    @Test
    void signUp_whenValidRequestData_shouldSendMessageWithCorrectUserData() {
        SignUpRequest signUpRequest = getSignUpRequest();
        UserResponse userResponse = getUserResponse();
        AuthResponse authResponse = getAuthResponse();

        doReturn(userResponse).when(userService).createUser(signUpRequest);
        doReturn(authResponse).when(tokenService).generateTokens(userResponse.username(), userResponse.id(), response);

        authService.signUp(signUpRequest, response);

        ArgumentCaptor<MessageDto> captor = ArgumentCaptor.forClass(MessageDto.class);
        verify(kafkaProducerService).sendMessageDto(eq(userResponse.id()), captor.capture());

        MessageDto messageDto = captor.getValue();

        assertEquals(userResponse.email(), messageDto.getEmail());
    }

    @Test
    void signIn_whenValidRequestData_shouldReturnAuthResponse() {
        Authentication authentication = mock(Authentication.class);

        SignInRequest signInRequest = getSignInRequest();
        CustomUserDetails customUserDetails = new CustomUserDetails(ID, USERNAME, EMAIL, "");
        AuthResponse authResponse = getAuthResponse();

        doReturn(authentication).when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        doReturn(customUserDetails).when(authentication).getPrincipal();
        doNothing().when(tokenService).deleteRefreshToken(anyLong());
        doReturn(authResponse).when(tokenService)
                .generateTokens(customUserDetails.username(), customUserDetails.id(), response);

        AuthResponse actual = authService.signIn(signInRequest, response);

        Assertions.assertEquals(authResponse, actual);
        Assertions.assertEquals(authResponse.accessToken(), actual.accessToken());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(authentication).getPrincipal();
        verify(tokenService).deleteRefreshToken(anyLong());
        verify(tokenService).generateTokens(customUserDetails.username(), customUserDetails.id(), response);
    }

    @Test
    void signIn_whenInvalidRequestData_shouldThrowAuthException() {
        SignInRequest signInRequest = getSignInRequest();

        doThrow(BadCredentialsException.class).when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(AuthException.class, () -> authService.signIn(signInRequest, response));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService, never()).deleteRefreshToken(anyLong());
        verify(tokenService, never()).generateTokens(anyString(), anyLong(), any(HttpServletResponse.class));
    }

    @Test
    void refreshToken_whenValidRequestData_shouldReturnAuthResponse() {
        String refreshToken = "refresh_1aBcDeFgHiJkLmNoPqRsTuVwXyZ_abcdef1234567890";
        AuthResponse authResponse = getAuthResponse();

        doReturn(authResponse).when(tokenService).validateAndRefreshToken(refreshToken, response);

        AuthResponse actual = authService.refreshToken(refreshToken, response);

        assertEquals(authResponse, actual);
        assertEquals(authResponse.accessToken(), actual.accessToken());

        verify(tokenService).validateAndRefreshToken(refreshToken, response);
    }

    @Test
    void refreshToken_whenTokenServiceThrowException_shouldAuthException() {
        doThrow(RuntimeException.class).when(tokenService).validateAndRefreshToken(anyString(), any());

        assertThrows(RuntimeException.class, () -> authService.refreshToken(anyString(), any()));

        verify(tokenService).validateAndRefreshToken(anyString(), any());
    }

    private AuthResponse getAuthResponse() {
        return new AuthResponse(ACCESS_TOKEN);
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
