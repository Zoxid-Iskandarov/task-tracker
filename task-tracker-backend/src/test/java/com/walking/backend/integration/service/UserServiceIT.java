package com.walking.backend.integration.service;

import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.user.*;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.InvalidFileException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.User;
import com.walking.backend.domain.model.UserProfile;
import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.integration.annotation.WithMockUser;
import com.walking.backend.integration.util.MinioTestHelper;
import com.walking.backend.props.AppProperties;
import com.walking.backend.repository.UserProfileRepository;
import com.walking.backend.repository.UserRepository;
import com.walking.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

@WithMockUser
@RequiredArgsConstructor
public class UserServiceIT extends IntegrationTestBase {
    private final UserService userService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    private final MinioTestHelper minioTestHelper;
    private final AppProperties appProperties;

    @Test
    void searchUsersToInvite_whenUserIsBoardMember_shouldReturnFilteredPage() {
        var pageable = PageRequest.of(0, 10);

        Page<UserSearchResponse> actual = userService.searchUsersToInvite(1L, "john", pageable);

        assertThat(actual.getContent()).isNotEmpty();
        assertThat(actual.getContent())
                .extracting(UserSearchResponse::username)
                .contains("john_doe");
        assertThat(actual.getContent())
                .extracting(UserSearchResponse::username)
                .doesNotContain("john_snow");
    }

    @Test
    void searchUsersToInvite_whenUserIsNotBoardMember_shouldThrowAccessDeniedException() {
        var pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> userService.searchUsersToInvite(99L, "john", pageable))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void searchUsersToInvite_whenNoUsersMatchQuery_shouldReturnEmptyPage() {
        var pageable = PageRequest.of(0, 10);

        Page<UserSearchResponse> result = userService.searchUsersToInvite(
                1L, "nonexistent_user", pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getCurrentUserProfileById_whenUserExists_shouldReturnProfile() {
        UserProfileResponse actual = userService.getCurrentUserProfileById(2L);

        assertThat(actual).isNotNull();
        assertThat(actual.displayName()).isEqualTo("Jane Smith");
    }

    @Test
    void getCurrentUserProfileById_whenCalledTwice_shouldUseCache() {
        UserProfileResponse first = userService.getCurrentUserProfileById(2L);

        userProfileRepository.findById(2L)
                .ifPresent(profile -> {
                    profile.setDisplayName("MODIFIED NAME");
                    userProfileRepository.save(profile);
                });

        UserProfileResponse second = userService.getCurrentUserProfileById(2L);

        assertThat(second.displayName()).isEqualTo(first.displayName());
        assertThat(second.displayName()).isNotEqualTo("MODIFIED NAME");
    }

    @Test
    void getUserById_whenUserNotFound_shouldThrowObjectNotFoundException() {
        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("User with id 99 not found");
    }

    @Test
    void getAssigneeByTaskIds_whenTasksHaveAssignees_shouldReturnGroupedMap() {
        Set<Long> taskIds = Set.of(1L, 2L, 3L);

        Map<Long, List<UserShortResponse>> result = userService.getAssigneeByTaskIds(taskIds);

        // Task 1:
        assertThat(result).containsKey(1L);
        assertThat(result.get(1L)).hasSize(2);
        assertThat(result.get(1L))
                .extracting(UserShortResponse::id)
                .containsExactlyInAnyOrder(1L, 2L);

        // Task 2:
        assertThat(result).containsKey(2L);
        assertThat(result.get(2L)).hasSize(1);
        assertThat(result.get(2L).getFirst().id()).isEqualTo(3L);

        // Task 3:
        assertThat(result).doesNotContainKey(3L);

        UserShortResponse assignee = result.get(1L).stream()
                .filter(u -> u.id() == 1L)
                .findFirst()
                .orElseThrow();
        assertThat(assignee.username()).isEqualTo("john_doe");
        assertThat(assignee.displayName()).isEqualTo("John Doe");
    }

    @Test
    void createUser_whenUsernameAlreadyExists_shouldThrowDuplicateException() {
        var signUpRequest = new SignUpRequest("john_doe", "some@gmail.com", "Password123");

        assertThatThrownBy(() -> userService.createUser(signUpRequest))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("Username %s is already taken".formatted(signUpRequest.username()));
    }

    @Test
    void createUser_whenEmailAlreadyExists_shouldThrowDuplicateException() {
        var signUpRequest = new SignUpRequest("some_user", "john.doe@example.com", "Password123");

        assertThatThrownBy(() -> userService.createUser(signUpRequest))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("Email %s is already taken".formatted(signUpRequest.email()));
    }

    @Test
    void createUser_whenValidRequestData_shouldReturnUserResponse() {
        var signUpRequest = new SignUpRequest("dante", "dante@gmail.com", "Password123");

        UserResponse actual = userService.createUser(signUpRequest);

        assertThat(actual.username()).isEqualTo(signUpRequest.username());
        assertThat(actual.email()).isEqualTo(signUpRequest.email());

        Optional<User> userOptional = userRepository.findByUsername(actual.username());
        assertThat(userOptional).isPresent();

        Optional<UserProfile> profileOptional = userProfileRepository.findById(userOptional.get().getId());
        assertThat(profileOptional).isPresent();
    }

    @Test
    void getUserProfileById_whenProfileNotFound_shouldThrowObjectNotFoundException() {
        Long userId = 99L;

        assertThatThrownBy(() -> userService.getUserProfileById(userId))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("User with id %d not found".formatted(userId));
    }

    @Test
    void getUserProfileById_whenProfileExists_shouldReturnUserPublicProfileResponse() {
        UserPublicProfileResponse actual = userService.getUserProfileById(1L);

        assertThat(actual).isNotNull();
        assertThat(actual.username()).isEqualTo("john_doe");
        assertThat(actual.displayName()).isEqualTo("John Doe");
    }

    @Test
    void getUserProfileById_whenCalledTwice_shouldUseCache() {
        UserPublicProfileResponse first = userService.getUserProfileById(1L);

        userProfileRepository.findById(1L)
                .ifPresent(profile -> {
                    profile.setDisplayName("MODIFIED PUBLIC NAME");
                    userProfileRepository.save(profile);
                });

        UserPublicProfileResponse second = userService.getUserProfileById(1L);

        assertThat(second.displayName()).isEqualTo(first.displayName());
        assertThat(second.displayName()).isNotEqualTo("MODIFIED PUBLIC NAME");
    }

    @Test
    void updateUserProfile_whenProfileExists_shouldUpdateAndReturnUserProfileResponse() {
        var updateUserProfileRequest = new UpdateUserProfileRequest("New Display Name", "Updated bio");

        UserProfileResponse actual = userService.updateUserProfile(1L, updateUserProfileRequest);

        assertThat(actual.displayName()).isEqualTo(updateUserProfileRequest.displayName());
        assertThat(actual.bio()).isEqualTo(updateUserProfileRequest.bio());

        UserProfile saved = userProfileRepository.findById(1L).orElseThrow();

        assertThat(saved.getDisplayName()).isEqualTo(updateUserProfileRequest.displayName());
        assertThat(saved.getBio()).isEqualTo(updateUserProfileRequest.bio());
    }

    @Test
    void updateUserProfile_whenProfileNotFound_shouldThrowObjectNotFoundException() {
        var updateUserProfileRequest = new UpdateUserProfileRequest("Name", "Bio");

        assertThatThrownBy(() -> userService.updateUserProfile(99L, updateUserProfileRequest))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("Profile with id 99 not found");
    }

    @Test
    void updateUserProfile_whenCalled_shouldEvictAllCaches() {
        userService.getCurrentUserProfileById(1L);
        userService.getUserProfileById(1L);
        userService.getUserShortById(1L);

        var updateUserProfileRequest = new UpdateUserProfileRequest("Evicted Name", "Evicted bio");
        userService.updateUserProfile(1L, updateUserProfileRequest);

        userProfileRepository.findById(1L)
                .ifPresent(profile -> {
                    profile.setDisplayName("DB DIRECT NAME");
                    userProfileRepository.save(profile);
                });

        UserProfileResponse refreshed = userService.getCurrentUserProfileById(1L);
        assertThat(refreshed.displayName()).isEqualTo("DB DIRECT NAME");
    }

    @Test
    void uploadAvatar_whenValidImage_shouldUploadAndSaveUrl() {
        var file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "fake-image-content".getBytes());

        UserProfileResponse actual = userService.uploadAvatar(1L, file);

        assertThat(actual.avatarUrl()).isNotBlank();

        UserProfile saved = userProfileRepository.findById(1L).orElseThrow();

        assertThat(saved.getAvatarUrl()).isEqualTo(actual.avatarUrl());
        assertThat(avatarExists(saved.getAvatarUrl())).isTrue();
    }

    @Test
    void uploadAvatar_whenAvatarAlreadyExists_shouldDeleteOldAndUploadNew() {
        MockMultipartFile firstFile = new MockMultipartFile(
                "file", "first.png", "image/png", "first-content".getBytes());
        UserProfileResponse first = userService.uploadAvatar(1L, firstFile);
        String oldAvatarUrl = first.avatarUrl();

        MockMultipartFile secondFile = new MockMultipartFile(
                "file", "second.png", "image/png", "second-content".getBytes());
        UserProfileResponse second = userService.uploadAvatar(1L, secondFile);

        assertThat(avatarExists(oldAvatarUrl)).isFalse();
        assertThat(avatarExists(second.avatarUrl())).isTrue();

        UserProfile saved = userProfileRepository.findById(1L).orElseThrow();
        assertThat(saved.getAvatarUrl()).isEqualTo(second.avatarUrl());
    }

    @Test
    void uploadAvatar_whenFileIsEmpty_shouldThrowInvalidFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> userService.uploadAvatar(1L, emptyFile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("File is empty");
    }

    @Test
    void uploadAvatar_whenFileIsNull_shouldThrowInvalidFileException() {
        assertThatThrownBy(() -> userService.uploadAvatar(1L, null))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("File is empty");
    }

    @Test
    void uploadAvatar_whenFileIsNotImage_shouldThrowInvalidFileException() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "pdf-content".getBytes());

        assertThatThrownBy(() -> userService.uploadAvatar(1L, pdfFile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("File is not an image");
    }

    @Test
    void uploadAvatar_whenProfileNotFound_shouldThrowObjectNotFoundException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "content".getBytes());

        assertThatThrownBy(() -> userService.uploadAvatar(99L, file))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("Profile with id 99 not found");
    }

    @Test
    void uploadAvatar_whenCalled_shouldEvictAllCaches() {
        userService.getCurrentUserProfileById(1L);
        userService.getUserProfileById(1L);
        userService.getUserShortById(1L);

        MockMultipartFile file = new MockMultipartFile(
                "file", "cache-test.png", "image/png", "content".getBytes());
        userService.uploadAvatar(1L, file);

        userProfileRepository.findById(1L)
                .ifPresent(p -> {
                    p.setDisplayName("FRESH FROM DB");
                    userProfileRepository.save(p);
                });

        UserProfileResponse refreshed = userService.getCurrentUserProfileById(1L);
        assertThat(refreshed.displayName()).isEqualTo("FRESH FROM DB");
    }

    @Test
    void deleteAvatar_whenAvatarExists_shouldDeleteFromMinioAndSetNull() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "to-delete.png", "image/png", "content".getBytes());
        UserProfileResponse uploaded = userService.uploadAvatar(1L, file);
        String avatarUrl = uploaded.avatarUrl();

        userService.deleteAvatar(1L);

        assertThat(avatarExists(avatarUrl)).isFalse();

        UserProfile saved = userProfileRepository.findById(1L).orElseThrow();
        assertThat(saved.getAvatarUrl()).isNull();
    }

    @Test
    void deleteAvatar_whenNoAvatar_shouldNotCallFileStorage() {
        userProfileRepository.findById(3L)
                .ifPresent(p -> {
                    p.setAvatarUrl(null);
                    userProfileRepository.save(p);
                });

        assertThatCode(() -> userService.deleteAvatar(3L))
                .doesNotThrowAnyException();

        UserProfile saved = userProfileRepository.findById(3L).orElseThrow();
        assertThat(saved.getAvatarUrl()).isNull();
    }

    @Test
    void deleteAvatar_whenProfileNotFound_shouldThrowObjectNotFoundException() {
        assertThatThrownBy(() -> userService.deleteAvatar(99L))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("Profile with id 99 not found");
    }

    @Test
    void deleteAvatar_whenCalled_shouldEvictAllCaches() {
        userService.getCurrentUserProfileById(1L);
        userService.getUserProfileById(1L);
        userService.getUserShortById(1L);

        userService.deleteAvatar(1L);

        userProfileRepository.findById(1L)
                .ifPresent(p -> {
                    p.setDisplayName("AFTER DELETE AVATAR");
                    userProfileRepository.save(p);
                });

        UserProfileResponse refreshed = userService.getCurrentUserProfileById(1L);
        assertThat(refreshed.displayName()).isEqualTo("AFTER DELETE AVATAR");
    }

    @AfterEach
    void clearAvatarBucket() {
        minioTestHelper.clearBucket(appProperties.getMinio().getBucketAvatar());
    }

    private boolean avatarExists(String objectName) {
        return minioTestHelper.objectExists(appProperties.getMinio().getBucketAvatar(), objectName);
    }
}
