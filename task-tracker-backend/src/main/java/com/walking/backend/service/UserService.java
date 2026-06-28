package com.walking.backend.service;

import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.user.*;
import com.walking.backend.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserService {

    Page<UserSearchResponse> searchUsersToInvite(Long boardId, String query, Pageable pageable);

    UserProfileResponse getCurrentUserProfileById(Long userId);

    User getProxyUserById(Long userId);

    User getUserById(Long userId);

    Set<User> getBoardMembersForTask(Long sectionId, Set<Long> assigneeIds);

    List<UserShortResponse> getUserShortsByIds(Set<Long> userIds);

    UserShortResponse getUserShortById(Long userId);

    Map<Long, List<UserShortResponse>> getAssigneeByTaskIds(Set<Long> taskIds);

    UserResponse createUser(SignUpRequest signUpRequest);

    UserPublicProfileResponse getUserProfileById(Long userId);

    UserProfileResponse updateUserProfile(Long userId, UpdateUserProfileRequest updateUserProfileRequest);

    UserProfileResponse uploadAvatar(Long userId, MultipartFile file);

    void deleteAvatar(Long userId);
}
