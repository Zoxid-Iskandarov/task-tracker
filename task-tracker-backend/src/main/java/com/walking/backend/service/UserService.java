package com.walking.backend.service;

import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.user.*;
import com.walking.backend.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    Page<UserSearchResponse> searchUsersToInvite(Long boardId, String query, Pageable pageable);

    UserProfileResponse getCurrentUserProfileById(Long userId);

    UserResponse getUserByUsername(String username);

    User getProxyUserById(Long userId);

    User getUserById(Long userId);

    UserResponse createUser(SignUpRequest signUpRequest);

    UserPublicProfileResponse getUserProfileById(Long userId);

    UserProfileResponse updateUserProfile(Long userId, UpdateUserProfileRequest updateUserProfileRequest);

    void uploadAvatar(Long userId, MultipartFile file);

    void deleteAvatar(Long userId);
}
