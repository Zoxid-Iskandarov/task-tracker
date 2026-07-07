package com.walking.backend.service;

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
import com.walking.backend.service.impl.UserServiceImpl;
import com.walking.backend.service.mapper.user.SignUpRequestMapper;
import com.walking.backend.service.mapper.user.UserProfileResponseMapper;
import com.walking.backend.service.mapper.user.UserResponseMapper;
import com.walking.backend.storage.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    private static final Long ID = 1L;
    private static final String USERNAME = "Dante";
    private static final String EMAIL = "dante@gmail.com";
    private static final String DISPLAY_NAME = "DevilMayCry";
    private static final String PASSWORD = "Password123";
    private static final String ENCODED_PASSWORD = "EncodedPassword";
    private static final String AVATAR_URL = "avatars/1/08a90e22-5bdc-48bc-905a-7ed933d93ec7";
    private static final String BIO = "Bio";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UserResponseMapper userResponseMapper;

    @Mock
    private SignUpRequestMapper signUpRequestMapper;

    @Mock
    private UserProfileResponseMapper userProfileResponseMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void searchUsersToInvite_whenValidRequestData_shouldReturnPageOfUserSearchResponses() {
        var user1 =
                new UserSearchResponse(1L, "Dante", "1.image");
        var user2 =
                new UserSearchResponse(2L, "Daniel", "2.image");
        var pageable = PageRequest.of(0, 10);
        var userSearchResponses = new PageImpl<>(List.of(user1, user2), pageable, 1L);

        doReturn(userSearchResponses).when(userRepository).searchUsersByQueryAndExcludeBoardMembers("Dan", ID, pageable);

        Page<UserSearchResponse> actual = userService.searchUsersToInvite(ID, "Dan", pageable);

        assertTrue(actual.hasContent());
        assertEquals(2, actual.getContent().size());
        assertEquals(user1, actual.getContent().get(0));
        assertEquals(user2, actual.getContent().get(1));

        verify(userRepository).searchUsersByQueryAndExcludeBoardMembers("Dan", ID, pageable);
    }

    @Test
    void getCurrentUserProfileById_whenValidRequestData_shouldReturnUserProfileResponse() {
        var user =
                new UserProfileResponse(ID, USERNAME, EMAIL, DISPLAY_NAME, AVATAR_URL, BIO);

        doReturn(Optional.of(user)).when(userProfileRepository).findUserProfileByUserId(user.id());

        UserProfileResponse actual = userService.getCurrentUserProfileById(user.id());

        assertEquals(user, actual);

        verify(userProfileRepository).findUserProfileByUserId(user.id());
    }

    @Test
    void getCurrentUserProfileById_whenInvalidRequestData_shouldThrowObjectNotFoundException() {
        doThrow(ObjectNotFoundException.class).when(userProfileRepository).findUserProfileByUserId(ID);

        assertThrows(ObjectNotFoundException.class, () -> userService.getCurrentUserProfileById(ID));

        verify(userProfileRepository).findUserProfileByUserId(ID);
    }

    @Test
    void getUserById_whenUserExists_returnUser() {
        var user = getSavedUser();

        doReturn(Optional.of(user)).when(userRepository).findById(user.getId());

        User actual = userService.getUserById(user.getId());

        assertEquals(user.getId(), actual.getId());
        assertEquals(user.getUsername(), actual.getUsername());
        assertEquals(user.getEmail(), actual.getEmail());
        assertEquals(user.getPassword(), actual.getPassword());

        verify(userRepository).findById(user.getId());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void getUserById_whenUserNotFound_throwObjectNotFoundException() {
        doReturn(Optional.empty()).when(userRepository).findById(ID);

        assertThrows(ObjectNotFoundException.class, () -> userService.getUserById(ID));

        verify(userRepository).findById(ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void getAssigneeByTaskIds_whenMultipleAssignees_shouldReturnGroupByTaskId() {
        var taskIds = Set.of(1L, 2L);
        var projection1 = new TaskAssigneeProjection(
                1L, 1L, "user1", "user1DisplayName", "avatar1");
        var projection2 = new TaskAssigneeProjection(
                2L, 2L, "user2", "user2DisplayName", "avatar2");
        var projection3 = new TaskAssigneeProjection(
                1L, 3L, "user3", "user3DisplayName", "avatar3");

        doReturn(List.of(projection1, projection2, projection3)).when(userProfileRepository)
                .findAssigneeProjectionByTaskIds(taskIds);

        Map<Long, List<UserShortResponse>> actual = userService.getAssigneeByTaskIds(taskIds);

        assertEquals(2, actual.size());
        assertEquals(2, actual.get(1L).size());
        assertEquals(1, actual.get(2L).size());
        assertTrue(actual.get(1L).stream()
                .anyMatch(u -> u.id().equals(1L) && u.username().equals("user1")));
        assertTrue(actual.get(2L).stream()
                .anyMatch(u -> u.id().equals(2L) && u.username().equals("user2")));
        assertTrue(actual.get(1L).stream()
                .anyMatch(u -> u.id().equals(3L) && u.username().equals("user3")));

        verify(userProfileRepository).findAssigneeProjectionByTaskIds(taskIds);
    }

    @Test
    void getAssigneeByTaskIds_whenNoAssignees_shouldReturnEmptyMap() {
        var taskIds = Set.of(1L, 2L);

        doReturn(List.of()).when(userProfileRepository).findAssigneeProjectionByTaskIds(taskIds);

        Map<Long, List<UserShortResponse>> actual = userService.getAssigneeByTaskIds(taskIds);

        assertTrue(actual.isEmpty());

        verify(userProfileRepository).findAssigneeProjectionByTaskIds(taskIds);
    }

    @Test
    void createUser_whenValidRequestData_shouldReturnUserResponse() {
        var signUpRequest = getSignUpRequest();
        var user = getNewUser();
        var savedUser = getSavedUser();
        var userResponse = new UserResponse(ID, USERNAME, EMAIL);

        doReturn(Optional.empty()).when(userRepository).findByUsername(signUpRequest.username());
        doReturn(Optional.empty()).when(userRepository).findByEmail(signUpRequest.email());
        doReturn(user).when(signUpRequestMapper).toEntity(signUpRequest);
        doReturn(ENCODED_PASSWORD).when(passwordEncoder).encode(PASSWORD);
        doReturn(savedUser).when(userRepository).save(user);
        doReturn(userResponse).when(userResponseMapper).toDto(savedUser);

        UserResponse actual = userService.createUser(signUpRequest);

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);

        assertEquals(userResponse.id(), actual.id());
        assertEquals(userResponse.username(), actual.username());
        assertEquals(userResponse.email(), actual.email());

        verify(userProfileRepository).save(profileCaptor.capture());
        assertEquals(savedUser, profileCaptor.getValue().getUser());

        verify(userRepository).findByUsername(signUpRequest.username());
        verify(userRepository).findByEmail(signUpRequest.email());
        verify(signUpRequestMapper).toEntity(signUpRequest);
        verify(passwordEncoder).encode(PASSWORD);
        verify(userRepository).save(user);
        verify(userResponseMapper).toDto(savedUser);
    }

    @Test
    void createUser_whenUsernameAlreadyExists_shouldThrowDuplicateException() {
        var signUpRequest = getSignUpRequest();

        doReturn(Optional.of(getSavedUser())).when(userRepository).findByUsername(signUpRequest.username());

        assertThrows(DuplicateException.class, () -> userService.createUser(signUpRequest));

        verify(userRepository).findByUsername(signUpRequest.username());
        verify(userRepository, never()).findByEmail(anyString());
        verify(signUpRequestMapper, never()).toEntity(any(SignUpRequest.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(userResponseMapper, never()).toDto(any(User.class));

        verifyNoMoreInteractions(
                userRepository, signUpRequestMapper, passwordEncoder, userResponseMapper, userProfileRepository);
    }

    @Test
    void createUser_whenEmailAlreadyExists_shouldThrowDuplicateException() {
        var signUpRequest = getSignUpRequest();

        doReturn(Optional.empty()).when(userRepository).findByUsername(signUpRequest.username());
        doReturn(Optional.of(getSavedUser())).when(userRepository).findByEmail(signUpRequest.email());

        assertThrows(DuplicateException.class, () -> userService.createUser(signUpRequest));

        verify(userRepository).findByUsername(signUpRequest.username());
        verify(userRepository).findByEmail(signUpRequest.email());
        verify(signUpRequestMapper, never()).toEntity(any(SignUpRequest.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(userResponseMapper, never()).toDto(any(User.class));

        verifyNoMoreInteractions(
                userRepository, signUpRequestMapper, passwordEncoder, userResponseMapper, userProfileRepository);
    }

    @Test
    void getUserProfileById_whenValidRequestData_shouldReturnUserPublicProfileResponse() {
        var user = new UserPublicProfileResponse(ID, USERNAME, DISPLAY_NAME, AVATAR_URL, BIO);

        doReturn(Optional.of(user)).when(userProfileRepository).findUserPublicProfileByUserId(user.id());

        UserPublicProfileResponse actual = userService.getUserProfileById(user.id());

        assertEquals(user, actual);

        verify(userProfileRepository).findUserPublicProfileByUserId(user.id());
    }

    @Test
    void getUserProfileById_whenInvalidRequestData_shouldThrowObjectNotFoundException() {
        doThrow(ObjectNotFoundException.class).when(userProfileRepository).findUserPublicProfileByUserId(ID);

        assertThrows(ObjectNotFoundException.class, () -> userService.getUserProfileById(ID));

        verify(userProfileRepository).findUserPublicProfileByUserId(ID);
    }

    @Test
    void updateUserProfile_whenValidRequestData_shouldReturnUserProfileResponse() {
        var updateRequest = new UpdateUserProfileRequest("Updated Display Name", "Updated Bio");
        var userProfile = getUserProfile();
        var userProfileResponse = new UserProfileResponse(
                ID, USERNAME, EMAIL, updateRequest.displayName(), AVATAR_URL, updateRequest.bio());

        doReturn(Optional.of(userProfile)).when(userProfileRepository).findById(ID);
        doReturn(userProfile).when(userProfileRepository).save(userProfile);
        doReturn(userProfileResponse).when(userProfileResponseMapper).toDto(userProfile);

        UserProfileResponse actual = userService.updateUserProfile(ID, updateRequest);

        assertEquals(userProfileResponse, actual);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());

        assertEquals(updateRequest.displayName(), captor.getValue().getDisplayName());
        assertEquals(updateRequest.bio(), captor.getValue().getBio());

        verify(userProfileRepository).findById(ID);
        verify(userProfileResponseMapper).toDto(userProfile);
    }

    @Test
    void updateUserProfile_whenInvalidRequestData_shouldThrowObjectNotFoundException() {
        var updateRequest = new UpdateUserProfileRequest("Updated Display Name", "Updated Bio");

        doReturn(Optional.empty()).when(userProfileRepository).findById(ID);

        assertThrows(ObjectNotFoundException.class, () -> userService.updateUserProfile(ID, updateRequest));

        verify(userProfileRepository).findById(ID);
        verify(userProfileRepository, never()).save(any(UserProfile.class));
        verify(userProfileResponseMapper, never()).toDto(any(UserProfile.class));
    }

    @Test
    void uploadAvatar_whenFileIsNull_shouldThrowInvalidFileException() {
        assertThrows(InvalidFileException.class, () -> userService.uploadAvatar(ID, null));

        verifyNoInteractions(userProfileRepository, fileStorageService, userProfileResponseMapper);
    }

    @Test
    void uploadAvatar_whenFileIsEmpty_shouldThrowInvalidFileException() {
        var file = mock(MultipartFile.class);
        doReturn(true).when(file).isEmpty();

        assertThrows(InvalidFileException.class, () -> userService.uploadAvatar(ID, file));

        verifyNoInteractions(userProfileRepository, fileStorageService, userProfileResponseMapper);
    }

    @Test
    void uploadAvatar_whenFileContentTypeIsEmpty_shouldThrowInvalidFileException() {
        var file = mock(MultipartFile.class);

        doReturn(false).when(file).isEmpty();
        doReturn(null).when(file).getContentType();

        assertThrows(InvalidFileException.class, () -> userService.uploadAvatar(ID, file));

        verifyNoInteractions(userProfileRepository, fileStorageService, userProfileResponseMapper);
    }

    @Test
    void uploadAvatar_whenFileContentTypeIsNotImage_shouldThrowInvalidFileException() {
        var file = mock(MultipartFile.class);

        doReturn(false).when(file).isEmpty();
        doReturn("application/pdf").when(file).getContentType();

        assertThrows(InvalidFileException.class, () -> userService.uploadAvatar(ID, file));

        verifyNoInteractions(userProfileRepository, fileStorageService, userProfileResponseMapper);
    }

    @Test
    void uploadAvatar_whenUserProfileNotFound_shouldThrowObjectNotFoundException() {
        var file = mock(MultipartFile.class);

        doReturn(false).when(file).isEmpty();
        doReturn("image/png").when(file).getContentType();
        doReturn(Optional.empty()).when(userProfileRepository).findById(ID);

        assertThrows(ObjectNotFoundException.class, () -> userService.uploadAvatar(ID, file));

        verify(userProfileRepository).findById(ID);
        verifyNoMoreInteractions(userProfileRepository, fileStorageService, userProfileResponseMapper);
    }

    @Test
    void uploadAvatar_whenOldAvatarExists_shouldDeleteOldAvatarBeforeUpload() {
        var file = mock(MultipartFile.class);
        var userProfile = getUserProfile();
        var userProfileResponse = new UserProfileResponse(
                ID, USERNAME, EMAIL, DISPLAY_NAME, "avatars/1/new-uuid", BIO);

        doReturn(false).when(file).isEmpty();
        doReturn("image/png").when(file).getContentType();
        doReturn(Optional.of(userProfile)).when(userProfileRepository).findById(ID);
        doReturn(userProfileResponse).when(userProfileResponseMapper).toDto(userProfile);

        UserProfileResponse actual = userService.uploadAvatar(ID, file);

        assertEquals(userProfileResponse, actual);

        verify(userProfileRepository).findById(ID);
        verify(fileStorageService).deleteAvatar(AVATAR_URL);
        verify(fileStorageService).uploadAvatar(ID, file);
        verify(userProfileRepository).save(userProfile);
        verify(userProfileResponseMapper).toDto(userProfile);
    }

    @Test
    void uploadAvatar_whenNoOldAvatar_shouldNotCallDeleteAvatar() {
        var file = mock(MultipartFile.class);
        var userProfile = getUserProfile();
        userProfile.setAvatarUrl(null);

        var userProfileResponse = new UserProfileResponse(ID, USERNAME, EMAIL, DISPLAY_NAME, AVATAR_URL, BIO);

        doReturn(false).when(file).isEmpty();
        doReturn("image/png").when(file).getContentType();
        doReturn(Optional.of(userProfile)).when(userProfileRepository).findById(ID);
        doReturn(userProfileResponse).when(userProfileResponseMapper).toDto(userProfile);

        UserProfileResponse actual = userService.uploadAvatar(ID, file);

        assertEquals(userProfileResponse, actual);
        assertNotNull(actual.avatarUrl());

        verify(userProfileRepository).findById(ID);
        verify(fileStorageService, never()).deleteAvatar(anyString());
        verify(fileStorageService).uploadAvatar(ID, file);
        verify(userProfileRepository).save(userProfile);
        verify(userProfileResponseMapper).toDto(userProfile);
    }

    @Test
    void deleteAvatar_whenUserProfileNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(userProfileRepository).findById(ID);

        assertThrows(ObjectNotFoundException.class, () -> userService.deleteAvatar(ID));

        verify(userProfileRepository).findById(ID);
        verify(fileStorageService, never()).deleteAvatar(anyString());
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void deleteAvatar_whenAvatarNotExists_shouldNotCallDeleteAndSeva() {
        var userProfile = getUserProfile();
        userProfile.setAvatarUrl(null);

        doReturn(Optional.of(userProfile)).when(userProfileRepository).findById(ID);

        userService.deleteAvatar(ID);

        assertNull(userProfile.getAvatarUrl());

        verify(userProfileRepository).findById(ID);
        verify(fileStorageService, never()).deleteAvatar(anyString());
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void deleteAvatar_whenAvatarExists_shouldDeleteAvatar() {
        var userProfile = getUserProfile();

        doReturn(Optional.of(userProfile)).when(userProfileRepository).findById(ID);

        userService.deleteAvatar(ID);

        verify(userProfileRepository).findById(ID);
        verify(fileStorageService).deleteAvatar(AVATAR_URL);
        verify(userProfileRepository).save(userProfile);
    }

    private User getNewUser() {
        return new User(ID, USERNAME, EMAIL, PASSWORD);
    }

    private User getSavedUser() {
        return new User(ID, USERNAME, EMAIL, ENCODED_PASSWORD);
    }

    private SignUpRequest getSignUpRequest() {
        return new SignUpRequest(USERNAME, EMAIL, PASSWORD);
    }

    private UserProfile getUserProfile() {
        return new UserProfile(
                ID, DISPLAY_NAME, AVATAR_URL, BIO, getSavedUser(), LocalDateTime.now(), LocalDateTime.now());
    }
}
