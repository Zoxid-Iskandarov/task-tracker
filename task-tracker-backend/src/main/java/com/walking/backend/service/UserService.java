package com.walking.backend.service;

import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.domain.model.User;

public interface UserService {
    UserResponse getUserByUsername(String username);

    User getUserById(Long userId);

    UserResponse createUser(SignUpRequest signUpRequest);
}
