package com.walking.backend.service;

import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    Page<UserResponse> searchUsersToInvite(Long boardId, String query, Pageable pageable);

    UserResponse getUserByUsername(String username);

    User getProxyUserById(Long userId);

    User getUserById(Long userId);

    UserResponse createUser(SignUpRequest signUpRequest);
}
