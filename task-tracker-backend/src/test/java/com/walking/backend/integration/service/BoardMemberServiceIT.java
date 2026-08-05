package com.walking.backend.integration.service;

import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.boardMember.BoardMemberFilter;
import com.walking.backend.domain.dto.boardMember.BoardMemberRequest;
import com.walking.backend.domain.dto.boardMember.BoardMemberResponse;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.IllegalOperationException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.BoardMember;
import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.integration.annotation.WithMockUser;
import com.walking.backend.repository.BoardMemberRepository;
import com.walking.backend.security.principal.CustomUserDetails;
import com.walking.backend.service.BoardMemberService;
import com.walking.backend.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import static com.walking.backend.domain.model.ActivityType.MEMBER_REMOVED;
import static com.walking.backend.domain.model.ActivityType.MEMBER_ROLE_CHANGED;
import static com.walking.backend.domain.model.BoardRole.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@WithMockUser
@RequiredArgsConstructor
public class BoardMemberServiceIT extends IntegrationTestBase {
    private final BoardMemberService boardMemberService;
    private final ActivityService activityService;
    private final KafkaProducerService kafkaProducerService;

    private final BoardMemberRepository boardMemberRepository;

    @Test
    void getMembers_whenBoardExistsAndUserHasAccess_shouldReturnFilteredMembersPage() {
        var filter = new BoardMemberFilter("jan", null, null, null, null);
        var pageable = PageRequest.of(0, 10);

        Page<BoardMemberResponse> actual = boardMemberService.getMembers(1L, filter, pageable);

        assertThat(actual.getContent()).isNotEmpty();
        assertThat(actual.getContent())
                .extracting(BoardMemberResponse::username)
                .contains("jane_smith");
    }

    @Test
    void getMembers_whenNoMembersMatchFilter_shouldReturnEmptyPage() {
        var filter = new BoardMemberFilter("nonexistent_user_xyz", null, null, null, null);
        var pageable = PageRequest.of(0, 10);

        Page<BoardMemberResponse> actual = boardMemberService.getMembers(1L, filter, pageable);

        assertThat(actual.getContent()).isEmpty();
        assertThat(actual.getTotalElements()).isZero();
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void getMembers_whenUserHasNoAccess_shouldThrowAccessDeniedException() {
        var filter = new BoardMemberFilter(null, null, null, null, null);
        var pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> boardMemberService.getMembers(1L, filter, pageable))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getById_whenMemberExists_shouldReturnBoardMember() {
        BoardMember member = boardMemberService.getById(1L, 2L);

        assertThat(member).isNotNull();
        assertThat(member.getId().getBoardId()).isEqualTo(1L);
        assertThat(member.getId().getUserId()).isEqualTo(2L);
        assertThat(member.getRole()).isEqualTo(OWNER);
    }

    @Test
    void addMember_whenUserNotFound_shouldThrowObjectNotFoundException() {
        Long boardId = 1L;
        var request = new BoardMemberRequest(99L, EDITOR);
        var userDetails = new CustomUserDetails(2L, "jane_smith", "jane.smith@example.com", "password");

        assertThatThrownBy(() -> boardMemberService.addMember(boardId, request, userDetails))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("User with id 99 not found");
    }

    @Test
    void getById_whenMemberNotFound_shouldThrowObjectNotFoundException() {
        assertThatThrownBy(() -> boardMemberService.getById(1L, 99L))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Member with id 99 in board with id 1 not found");
    }

    @Test
    void addMember_whenValidRequestAndUserIsOwner_shouldAddMemberAndPublishActivityAndSendKafkaMessage() {
        Long boardId = 1L;
        // User 1 (john_doe) is not a member of board 1 yet
        var request = new BoardMemberRequest(1L, EDITOR);
        var userDetails = new CustomUserDetails(2L, "jane_smith", "jane.smith@example.com", "password");

        BoardMemberResponse response = boardMemberService.addMember(boardId, request, userDetails);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.role()).isEqualTo(EDITOR);

        assertThat(boardMemberRepository.existsByIdBoardIdAndIdUserId(boardId, 1L)).isTrue();
        verify(kafkaProducerService).sendMessageDto(eq(1L), any());
    }

    @Test
    void addMember_whenUserAlreadyMember_shouldThrowDuplicateException() {
        Long boardId = 1L;
        // User 3 is already a member of board 1
        var request = new BoardMemberRequest(3L, EDITOR);
        var userDetails = new CustomUserDetails(2L, "jane_smith", "jane.smith@example.com", "password");

        assertThatThrownBy(() -> boardMemberService.addMember(boardId, request, userDetails))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("User with id 3 is already a member of this board");
    }

    @Test
    @WithMockUser(id = 3L, username = "john_snow") // EDITOR
    void addMember_whenUserNotOwner_shouldThrowAccessDeniedException() {
        Long boardId = 1L;
        var request = new BoardMemberRequest(1L, EDITOR);
        var userDetails = new CustomUserDetails(3L, "john_snow", "john.snow@example.com", "password");

        assertThatThrownBy(() -> boardMemberService.addMember(boardId, request, userDetails))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void removeMember_whenValidAndOwner_shouldRemoveMemberAndTaskAssigneesAndPublishActivity() {
        Long boardId = 1L;
        Long userIdToRemove = 3L; // john_snow is member of board 1
        Long currentUserId = 2L;

        boardMemberService.removeMember(boardId, userIdToRemove, currentUserId);

        assertThat(boardMemberRepository.existsByIdBoardIdAndIdUserId(boardId, userIdToRemove)).isFalse();
        verify(activityService).publish(any(), eq(MEMBER_REMOVED), eq("Removed member john_snow"));
    }

    @Test
    void removeMember_whenRemovingSelf_shouldThrowIllegalOperationException() {
        Long boardId = 1L;
        Long currentUserId = 2L;

        assertThatThrownBy(() -> boardMemberService.removeMember(boardId, 2L, currentUserId))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("You cannot remove yourself from the board");
    }

    @Test
    void removeMember_whenMemberNotFound_shouldThrowObjectNotFoundException() {
        Long boardId = 1L;
        Long nonExistentUserId = 99L;
        Long currentUserId = 2L;

        assertThatThrownBy(() -> boardMemberService.removeMember(boardId, nonExistentUserId, currentUserId))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("Member with id 99 in board with id 1 not found");
    }

    @Test
    @WithMockUser(id = 3L, username = "john_snow")
    void removeMember_whenUserNotOwner_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> boardMemberService.removeMember(1L, 2L, 3L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void changeRole_whenValidAndManager_shouldChangeRoleAndPublishActivity() {
        Long boardId = 1L;
        Long userId = 3L; // john_snow (EDITOR)
        var request = new BoardMemberRequest(userId, VIEWER);
        Long currentUserId = 2L;

        BoardMemberResponse response = boardMemberService.changeRole(boardId, request, currentUserId);

        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo(VIEWER);

        BoardMember updated = boardMemberRepository.findByIdBoardIdAndIdUserId(boardId, userId).orElseThrow();
        assertThat(updated.getRole()).isEqualTo(VIEWER);

        verify(activityService).publish(any(), eq(MEMBER_ROLE_CHANGED), eq("Changed role for john_snow from EDITOR to VIEWER"));
    }

    @Test
    void changeRole_whenChangingOwnRole_shouldThrowIllegalOperationException() {
        Long boardId = 1L;
        var request = new BoardMemberRequest(2L, EDITOR);

        assertThatThrownBy(() -> boardMemberService.changeRole(boardId, request, 2L))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("You cannot change your own role");
    }

    @Test
    @WithMockUser(id = 3L, username = "john_snow")
    void changeRole_whenUserNotManager_shouldThrowAccessDeniedException() {
        var request = new BoardMemberRequest(2L, EDITOR);
        assertThatThrownBy(() -> boardMemberService.changeRole(1L, request, 3L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(id = 3L, username = "john_snow")
    void leaveBoard_whenValidMember_shouldLeaveBoardAndRemoveTaskAssignees() {
        Long boardId = 1L;
        Long currentUserId = 3L;

        boardMemberService.leaveBoard(boardId, currentUserId);

        assertThat(boardMemberRepository.existsByIdBoardIdAndIdUserId(boardId, currentUserId)).isFalse();
    }

    @Test
    void leaveBoard_whenLastOwnerTriesToLeave_shouldThrowIllegalOperationException() {
        Long boardId = 1L;
        Long currentUserId = 2L; // jane_smith is the only owner of board 1

        assertThatThrownBy(() -> boardMemberService.leaveBoard(boardId, currentUserId))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Last owner cannot leave the board");
    }

    @Test
    @WithMockUser(id = 3L, username = "john_snow")
    void leaveBoard_whenNotLastOwner_shouldLeaveSuccessfully() {
        // john_snow (3) - EDITOR
        Long boardId = 1L;
        Long currentUserId = 3L;

        boardMemberService.leaveBoard(boardId, currentUserId);

        assertThat(boardMemberRepository.existsByIdBoardIdAndIdUserId(boardId, currentUserId)).isFalse();
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void leaveBoard_whenUserNotMember_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> boardMemberService.leaveBoard(1L, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
