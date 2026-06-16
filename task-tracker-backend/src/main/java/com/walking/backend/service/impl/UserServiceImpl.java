package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.user.*;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.InvalidFileException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.User;
import com.walking.backend.domain.model.UserProfile;
import com.walking.backend.domain.projection.TaskAssigneeProjection;
import com.walking.backend.repository.UserProfileRepository;
import com.walking.backend.repository.UserRepository;
import com.walking.backend.service.FileStorageService;
import com.walking.backend.service.UserService;
import com.walking.backend.service.mapper.user.SignUpRequestMapper;
import com.walking.backend.service.mapper.user.UserProfileResponseMapper;
import com.walking.backend.service.mapper.user.UserResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final UserResponseMapper userResponseMapper;
    private final SignUpRequestMapper signUpRequestMapper;
    private final UserProfileResponseMapper userProfileResponseMapper;

    @Override
    @PreAuthorize("@resourceAccessService.canViewBoard(#boardId, principal.id)")
    public Page<UserSearchResponse> searchUsersToInvite(Long boardId, String query, Pageable pageable) {
        return userRepository.searchUsersByQueryAndExcludeBoardMembers(query, boardId, pageable);
    }

    @Override
    public UserProfileResponse getCurrentUserProfileById(Long userId) {
        return userProfileRepository.findUserProfileByUserId(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User with id %d not found"));
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userResponseMapper::toDto)
                .orElseThrow(() -> new ObjectNotFoundException("User %s not found".formatted(username)));
    }

    @Override
    public User getProxyUserById(Long userId) {
        return userRepository.getReferenceById(userId);
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User with id %d not found".formatted(userId)));
    }

    @Override
    public Set<User> getBoardMembersForTask(Long sectionId, Set<Long> assigneeIds) {
        return userRepository.findAllBySectionIdAndAssigneeIds(sectionId, assigneeIds);
    }

    @Override
    public List<UserShortResponse> getUserShortsByIds(Set<Long> userIds) {
        return userProfileRepository.findUserShortsByIds(userIds);
    }

    @Override
    public Map<Long, List<UserShortResponse>> getAssigneeByTaskIds(Set<Long> taskIds) {
        return userProfileRepository.findAssigneeProjectionByTaskIds(taskIds)
                .stream()
                .collect(Collectors.groupingBy(
                        TaskAssigneeProjection::taskId,
                        Collectors.mapping(
                                p ->
                                        new UserShortResponse(p.userId(), p.username(), p.displayName(), p.avatarUrl()),
                                Collectors.toList())
                        )
                );
    }

    @Override
    @Transactional
    public UserResponse createUser(SignUpRequest signUpRequest) {
        if (userRepository.findByUsername(signUpRequest.username()).isPresent()) {
            throw new DuplicateException("Username %s is already taken".formatted(signUpRequest.username()));
        }

        if (userRepository.findByEmail(signUpRequest.email()).isPresent()) {
            throw new DuplicateException("Email %s is already taken".formatted(signUpRequest.email()));
        }

        User user = signUpRequestMapper.toEntity(signUpRequest);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);

        UserProfile userProfile = new UserProfile();
        userProfile.setUser(savedUser);

        userProfileRepository.save(userProfile);

        return userResponseMapper.toDto(savedUser);
    }

    @Override
    public UserPublicProfileResponse getUserProfileById(Long userId) {
        return userProfileRepository.findUserPublicProfileByUserId(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User with id %d not found".formatted(userId)));
    }

    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(Long userId, UpdateUserProfileRequest updateUserProfileRequest) {
        return userProfileRepository.findById(userId)
                .map(profile -> {
                    profile.setDisplayName(updateUserProfileRequest.displayName());
                    profile.setBio(updateUserProfileRequest.bio());
                    return userProfileRepository.save(profile);
                })
                .map(userProfileResponseMapper::toDto)
                .orElseThrow(() -> new ObjectNotFoundException("Profile with id %d not found"));
    }

    @Override
    @Transactional
    public UserProfileResponse uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new InvalidFileException("File is not an image");
        }

        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ObjectNotFoundException("Profile with id %d not found".formatted(userId)));

        if (userProfile.getAvatarUrl() != null) {
            fileStorageService.delete(userProfile.getAvatarUrl());
        }

        String fileName = fileStorageService.upload(userId, file);

        userProfile.setAvatarUrl(fileName);
        userProfileRepository.save(userProfile);

        return userProfileResponseMapper.toDto(userProfile);
    }

    @Override
    @Transactional
    public void deleteAvatar(Long userId) {
        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ObjectNotFoundException("Profile with id %d not found".formatted(userId)));

        if (userProfile.getAvatarUrl() != null) {
            fileStorageService.delete(userProfile.getAvatarUrl());
            userProfile.setAvatarUrl(null);
            userProfileRepository.save(userProfile);
        }
    }
}
