package com.walking.backend.integration.service;

import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.User;
import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.repository.UserRepository;
import com.walking.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@RequiredArgsConstructor
public class UserServiceIT extends IntegrationTestBase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "Zoxka";
    private static final String EMAIL = "san781617@gmail.com";

    @Test
    void getUserByUsername_whenUserExists_returnUserResponse() {
        UserResponse userResponse = userService.getUserByUsername("Zoxka");

        assertNotNull(userResponse);
        assertThat(userResponse.id()).isEqualTo(USER_ID);
        assertThat(userResponse.username()).isEqualTo(USERNAME);
        assertThat(userResponse.email()).isEqualTo(EMAIL);
    }

    @Test
    void getUserByUsername_whenUserNotFound_throwObjectNotFoundException() {
        assertThrows(ObjectNotFoundException.class, () -> userService.getUserByUsername("non-existent-username"));
    }

    @Test
    void getUserById_whenUserExists_returnUser() {
        User user = userService.getUserById(USER_ID);

        assertNotNull(user);
        assertThat(user.getId()).isEqualTo(USER_ID);
        assertThat(user.getUsername()).isEqualTo(USERNAME);
        assertThat(user.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void getUserById_whenUserNotFound_throwObjectNotFoundException() {
        assertThrows(ObjectNotFoundException.class, () -> userService.getUserById(1000L));
    }

    @Test
    void createUser_whenValidRequestData_returnUserResponse() {
        SignUpRequest signUpRequest = new SignUpRequest("Sveta", "sveta@gmail.com", "Sveta123");

        UserResponse userResponse = userService.createUser(signUpRequest);

        assertNotNull(userResponse);
        assertNotNull(userResponse.id());
        assertThat(userResponse.username()).isEqualTo(signUpRequest.username());
        assertThat(userResponse.email()).isEqualTo(signUpRequest.email());
    }

    @Test
    void createUser_whenUsernameAlreadyExists_throwDuplicateException() {
        SignUpRequest signUpRequest = new SignUpRequest(USERNAME, "sveta@gmail.com", "Sveta123");

        assertThrows(DuplicateException.class, () -> userService.createUser(signUpRequest));
    }

    @Test
    void createUser_whenEmailAlreadyExists_throwDuplicateException() {
        SignUpRequest signUpRequest = new SignUpRequest("Sveta", EMAIL, "Sveta123");

        assertThrows(DuplicateException.class, () -> userService.createUser(signUpRequest));
    }

    @Test
    void createUser_whenValidRequest_passwordIsEncoded() {
        SignUpRequest signUpRequest = new SignUpRequest("Sveta", "sveta@gmail.com", "Sveta123");

        UserResponse userResponse = userService.createUser(signUpRequest);

        userRepository.findById(userResponse.id()).ifPresent(savedUser -> {
            assertThat(savedUser.getPassword()).isNotEqualTo(signUpRequest.password());
            assertTrue(passwordEncoder.matches(signUpRequest.password(), savedUser.getPassword()));
        });
    }

    @Test
    void createUser_whenUsernameExists_userIsNotSaved() {
        long usersBefore = userRepository.count();

        SignUpRequest signUpRequest = new SignUpRequest(USERNAME, "sveta@gmail.com", "Sveta123");

        assertThrows(DuplicateException.class, () -> userService.createUser(signUpRequest));

        long usersAfter = userRepository.count();

        assertEquals(usersBefore, usersAfter);
    }
}
