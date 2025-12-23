package com.walking.backend.service;

import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.User;
import com.walking.backend.repository.UserRepository;
import com.walking.backend.service.impl.UserServiceImpl;
import com.walking.backend.service.mapper.user.SignUpRequestMapper;
import com.walking.backend.service.mapper.user.UserResponseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    private static final Long ID = 1L;
    private static final String USERNAME = "Test";
    private static final String EMAIL = "test@gmail.com";
    private static final String PASSWORD = "Password123";
    private static final String ENCODED_PASSWORD = "EncodedPassword";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserResponseMapper userResponseMapper;

    @Mock
    private SignUpRequestMapper signUpRequestMapper;

    @InjectMocks
    private UserServiceImpl userService;


    @Test
    void getUserByUsername_whenUserExists_returnUserResponse() {
        User user = getSavedUser();
        UserResponse expected = getUserResponse();

        doReturn(Optional.of(user)).when(userRepository).findByUsername(user.getUsername());
        doReturn(expected).when(userResponseMapper).toDto(user);

        UserResponse actual = userService.getUserByUsername(user.getUsername());

        assertEquals(expected.id(), actual.id());
        assertEquals(expected.username(), actual.username());
        assertEquals(expected.email(), actual.email());

        verify(userRepository).findByUsername(user.getUsername());
        verify(userResponseMapper).toDto(user);

        verifyNoMoreInteractions(userRepository, userResponseMapper);
    }

    @Test
    void getUserByUsername_whenUserNotFound_throwObjectNotFoundException() {
        doReturn(Optional.empty()).when(userRepository).findByUsername(anyString());

        assertThrows(ObjectNotFoundException.class, () -> userService.getUserByUsername(anyString()));

        verify(userRepository).findByUsername(anyString());
        verify(userResponseMapper, never()).toDto(any(User.class));

        verifyNoMoreInteractions(userRepository, userResponseMapper);
    }

    @Test
    void getUserById_whenUserExists_returnUser() {
        User expected = getSavedUser();

        doReturn(Optional.of(expected)).when(userRepository).findById(expected.getId());

        User actual = userService.getUserById(expected.getId());

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getUsername(), actual.getUsername());
        assertEquals(expected.getEmail(), actual.getEmail());
        assertEquals(expected.getPassword(), actual.getPassword());

        verify(userRepository).findById(expected.getId());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void getUserById_whenUserNotFound_throwObjectNotFoundException() {
        doReturn(Optional.empty()).when(userRepository).findById(anyLong());

        assertThrows(ObjectNotFoundException.class, () -> userService.getUserById(anyLong()));

        verify(userRepository).findById(anyLong());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void createUser_whenValidRequestData_returnUserResponse() {
        SignUpRequest request = getSignUpRequest();
        User unSavedUser = getUnSavedUser();
        User savedUser = getSavedUser();
        UserResponse expected = getUserResponse();

        doReturn(Optional.empty()).when(userRepository).findByUsername(request.username());
        doReturn(Optional.empty()).when(userRepository).findByEmail(request.email());
        doReturn(unSavedUser).when(signUpRequestMapper).toEntity(request);
        doReturn(ENCODED_PASSWORD).when(passwordEncoder).encode(anyString());
        doReturn(savedUser).when(userRepository).save(unSavedUser);
        doReturn(expected).when(userResponseMapper).toDto(savedUser);

        UserResponse actual = userService.createUser(request);

        assertEquals(expected.id(), actual.id());
        assertEquals(expected.username(), actual.username());
        assertEquals(expected.email(), actual.email());

        verify(userRepository).findByUsername(request.username());
        verify(userRepository).findByEmail(request.email());
        verify(signUpRequestMapper).toEntity(request);
        verify(passwordEncoder).encode(anyString());
        verify(userRepository).save(unSavedUser);
        verify(userResponseMapper).toDto(savedUser);

        verifyNoMoreInteractions(userRepository, signUpRequestMapper, passwordEncoder, userResponseMapper);
    }

    @Test
    void createUser_whenUsernameAlreadyExists_throwDuplicateException() {
        SignUpRequest signUpRequest = getSignUpRequest();

        doReturn(Optional.of(getSavedUser())).when(userRepository).findByUsername(signUpRequest.username());

        assertThrows(DuplicateException.class, () -> userService.createUser(signUpRequest));

        verify(userRepository).findByUsername(signUpRequest.username());
        verify(userRepository, never()).findByEmail(anyString());
        verify(signUpRequestMapper, never()).toEntity(any(SignUpRequest.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(userResponseMapper, never()).toDto(any(User.class));

        verifyNoMoreInteractions(userRepository, signUpRequestMapper, passwordEncoder, userResponseMapper);
    }

    @Test
    void createUser_whenEmailAlreadyExists_throwDuplicateException() {
        SignUpRequest signUpRequest = getSignUpRequest();

        doReturn(Optional.empty()).when(userRepository).findByUsername(signUpRequest.username());
        doReturn(Optional.of(getSavedUser())).when(userRepository).findByEmail(signUpRequest.email());

        assertThrows(DuplicateException.class, () -> userService.createUser(signUpRequest));

        verify(userRepository).findByUsername(signUpRequest.username());
        verify(userRepository).findByEmail(signUpRequest.email());
        verify(signUpRequestMapper, never()).toEntity(any(SignUpRequest.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(userResponseMapper, never()).toDto(any(User.class));

        verifyNoMoreInteractions(userRepository, signUpRequestMapper, passwordEncoder, userResponseMapper);
    }

    private User getUnSavedUser() {
        User user = new User();
        user.setUsername(USERNAME);
        user.setEmail(EMAIL);
        user.setPassword(PASSWORD);

        return user;
    }

    private User getSavedUser() {
        return new User(ID, USERNAME, EMAIL, ENCODED_PASSWORD);
    }

    private UserResponse getUserResponse() {
        return new UserResponse(ID, USERNAME, EMAIL);
    }

    private SignUpRequest getSignUpRequest() {
        return new SignUpRequest(USERNAME, EMAIL, PASSWORD);
    }
}
