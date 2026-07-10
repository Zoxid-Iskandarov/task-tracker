package com.walking.backend.service;

import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.boardMember.BoardMemberFilter;
import com.walking.backend.domain.dto.boardMember.BoardMemberRequest;
import com.walking.backend.domain.dto.boardMember.BoardMemberResponse;
import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.IllegalOperationException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.*;
import com.walking.backend.repository.BoardMemberRepository;
import com.walking.backend.repository.TaskRepository;
import com.walking.backend.security.principal.CustomUserDetails;
import com.walking.backend.service.impl.BoardMemberServiceImpl;
import com.walking.backend.service.mapper.boardMember.BoardMemberResponseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.walking.backend.domain.model.ActivityType.MEMBER_REMOVED;
import static com.walking.backend.domain.model.ActivityType.MEMBER_ROLE_CHANGED;
import static com.walking.backend.domain.model.BoardRole.EDITOR;
import static com.walking.backend.domain.model.BoardRole.OWNER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardMemberServiceTest {
    private static final Long ID = 1L;

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private BoardService boardService;

    @Mock
    private UserService userService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private ActivityService activityService;

    @Mock
    private BoardMemberResponseMapper boardMemberResponseMapper;

    @InjectMocks
    private BoardMemberServiceImpl boardMemberService;

    @Test
    void getMembers_whenValidRequestData_shouldReturnPageOfBoardMemberResponses() {
        var filter = new BoardMemberFilter(null, null, null, null, null);
        var pageable = PageRequest.of(0, 10);

        var member1 = buildBoardMember(1L);
        var member2 = buildBoardMember(2L);
        var boardMembers = new PageImpl<>(List.of(member1, member2), pageable, 2L);

        var memberResponse1 = buildBoardMemberResponse(1L);
        var memberResponse2 = buildBoardMemberResponse(2L);

        doReturn(boardMembers).when(boardMemberRepository).findAll(any(Specification.class), eq(pageable));
        doReturn(memberResponse1).when(boardMemberResponseMapper).toDto(member1);
        doReturn(memberResponse2).when(boardMemberResponseMapper).toDto(member2);

        Page<BoardMemberResponse> actual = boardMemberService.getMembers(ID, filter, pageable);

        assertTrue(actual.hasContent());
        assertEquals(2, actual.getContent().size());
        assertEquals(memberResponse1, actual.getContent().get(0));
        assertEquals(memberResponse2, actual.getContent().get(1));

        verify(boardMemberRepository).findAll(any(Specification.class), eq(pageable));
        verify(boardMemberResponseMapper).toDto(member1);
        verify(boardMemberResponseMapper).toDto(member2);
    }

    @Test
    void getById_whenValidRequestData_shouldReturnBoardMember() {
        var member = buildBoardMember(1L);

        doReturn(Optional.of(member)).when(boardMemberRepository).findByIdBoardIdAndIdUserId(ID, 1L);

        BoardMember actual = boardMemberService.getById(ID, 1L);

        assertEquals(member, actual);

        verify(boardMemberRepository).findByIdBoardIdAndIdUserId(ID, 1L);
    }

    @Test
    void getById_whenMemberNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(boardMemberRepository).findByIdBoardIdAndIdUserId(ID, ID);

        assertThrows(ObjectNotFoundException.class, () -> boardMemberService.getById(ID, ID));

        verify(boardMemberRepository).findByIdBoardIdAndIdUserId(ID, ID);
    }

    @Test
    void addMember_whenUserAlreadyMember_shouldThrowDuplicateException() {
        var request = new BoardMemberRequest(ID, EDITOR);
        var userDetails = buildCustomUserDetails();

        doReturn(true).when(boardMemberRepository).existsByIdBoardIdAndIdUserId(ID, request.userId());

        assertThrows(DuplicateException.class, () -> boardMemberService.addMember(ID, request, userDetails));

        verify(boardMemberRepository).existsByIdBoardIdAndIdUserId(ID, request.userId());
        verifyNoInteractions(boardService, userService, kafkaProducerService, boardMemberResponseMapper);
        verify(boardMemberRepository, never()).flush();
    }

    @Test
    void addMember_whenValidRequestData_shouldAddMemberAndReturnBoardMemberResponse() {
        var boardMemberRequest = new BoardMemberRequest(ID, OWNER);
        var userDetails = buildCustomUserDetails();
        var board = getBoard();
        var user = getUser();
        var boardMemberResponse = buildBoardMemberResponse(ID);

        doReturn(false).when(boardMemberRepository).existsByIdBoardIdAndIdUserId(ID, ID);
        doReturn(board).when(boardService).getProxyBoardById(ID);
        doReturn(user).when(userService).getUserById(ID);
        doReturn(boardMemberResponse).when(boardMemberResponseMapper).toDto(any(BoardMember.class));

        BoardMemberResponse actual = boardMemberService.addMember(ID, boardMemberRequest, userDetails);

        assertEquals(boardMemberResponse, actual);
        assertEquals(1, board.getMembers().size());

        for (BoardMember member : board.getMembers()) {
            assertEquals(board, member.getBoard());
            assertEquals(user, member.getUser());
            assertEquals(OWNER, member.getRole());
            verify(boardMemberResponseMapper).toDto(member);
        }

        verify(boardMemberRepository).existsByIdBoardIdAndIdUserId(ID, boardMemberRequest.userId());
        verify(boardMemberRepository).flush();

        ArgumentCaptor<MessageDto> captor = ArgumentCaptor.forClass(MessageDto.class);
        verify(kafkaProducerService).sendMessageDto(eq(ID), captor.capture());

        MessageDto messageDto = captor.getValue();

        assertEquals(user.getEmail(), messageDto.getEmail());
        assertEquals("You've been added to a board", messageDto.getTitle());
        assertTrue(messageDto.getMessage().contains(user.getUsername()));
        assertTrue(messageDto.getMessage().contains(board.getName()));
        assertTrue(messageDto.getMessage().contains(userDetails.username()));
    }

    @Test
    void removeMember_whenUserWantsToDeleteYourself_shouldThrowIllegalOperationException() {
        assertThrows(IllegalOperationException.class, () -> boardMemberService.removeMember(ID, ID, ID));

        verifyNoInteractions(taskRepository, boardMemberRepository, activityService);
    }

    @Test
    void removeMember_whenMemberNotFound_shouldThrowObjectNotFoundException() {
        Long userId = 2L;

        doReturn(Optional.empty()).when(boardMemberRepository).findByIdBoardIdAndIdUserId(ID, userId);

        assertThrows(ObjectNotFoundException.class, () -> boardMemberService.removeMember(ID, userId, ID));

        verify(boardMemberRepository).findByIdBoardIdAndIdUserId(ID, userId);
        verifyNoInteractions(taskRepository, activityService);
        verify(boardMemberRepository, never()).delete(any(BoardMember.class));
    }

    @Test
    void removeMember_whenValidRequestData_shouldRemoveMember() {
        var currentUserID = 2L;
        var board = getBoard();
        var user = getUser();
        var member = buildBoardMember(board, user);

        doReturn(Optional.of(member)).when(boardMemberRepository).findByIdBoardIdAndIdUserId(board.getId(), user.getId());

        boardMemberService.removeMember(board.getId(), user.getId(), currentUserID);

        verify(taskRepository).removeAssigneeFromBoardTasks(board.getId(), user.getId());
        verify(boardMemberRepository).delete(member);
        verify(activityService).publish(board, MEMBER_REMOVED, "Removed member Re23");
    }

    @Test
    void changeRole_whenUserWantsChangeHisOwnRole_shouldThrowIllegalOperationException() {
        var request = new BoardMemberRequest(ID, OWNER);

        assertThrows(IllegalOperationException.class, () -> boardMemberService.changeRole(ID, request, ID));

        verifyNoInteractions(boardMemberRepository, activityService, boardMemberResponseMapper);
    }

    @Test
    void changeRole_whenMemberNotFound_shouldThrowObjectNotFoundException() {
        var request = new BoardMemberRequest(2L, OWNER);

        doReturn(Optional.empty()).when(boardMemberRepository).findByIdBoardIdAndIdUserId(ID, request.userId());

        assertThrows(ObjectNotFoundException.class, () -> boardMemberService.changeRole(ID, request, ID));

        verify(boardMemberRepository, never()).flush();
        verifyNoInteractions(activityService, boardMemberResponseMapper);
    }

    @Test
    void changeRole_whenValidRequestData_shouldChangeRoleAndReturnBoardMemberResponse() {
        var board = getBoard();
        var user = getUser();
        var member = buildBoardMember(board, user);
        var request = new BoardMemberRequest(user.getId(), EDITOR);
        var boardMemberResponse = buildBoardMemberResponse(user, request.role());

        doReturn(Optional.of(member)).when(boardMemberRepository).findByIdBoardIdAndIdUserId(board.getId(), user.getId());
        doReturn(boardMemberResponse).when(boardMemberResponseMapper).toDto(member);

        BoardMemberResponse actual = boardMemberService.changeRole(board.getId(), request, 2L);

        assertEquals(boardMemberResponse, actual);
        assertEquals(EDITOR, member.getRole());

        verify(boardMemberRepository).flush();
        verify(activityService).publish(board, MEMBER_ROLE_CHANGED,
                "Changed role for %s from OWNER to EDITOR".formatted(user.getUsername()));
        verify(boardMemberResponseMapper).toDto(member);
    }

    @Test
    void leaveBoard_whenMemberNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(boardMemberRepository).findByIdBoardIdAndIdUserId(ID, ID);

        assertThrows(ObjectNotFoundException.class, () -> boardMemberService.leaveBoard(ID, ID));

        verify(boardMemberRepository).findByIdBoardIdAndIdUserId(ID, ID);
        verify(boardMemberRepository, never()).countByIdBoardIdAndRole(any(), any());
        verify(boardMemberRepository, never()).delete(any(BoardMember.class));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void leaveBoard_whenUserIsLastOwner_shouldThrowIllegalOperationException() {
        var member = buildBoardMember(ID);

        doReturn(Optional.of(member)).when(boardMemberRepository).findByIdBoardIdAndIdUserId(ID, ID);
        doReturn(1L).when(boardMemberRepository).countByIdBoardIdAndRole(ID, OWNER);

        assertThrows(IllegalOperationException.class, () -> boardMemberService.leaveBoard(ID, ID));

        verify(boardMemberRepository).findByIdBoardIdAndIdUserId(ID, ID);
        verify(boardMemberRepository).countByIdBoardIdAndRole(ID, OWNER);
        verify(taskRepository, never()).removeAssigneeFromBoardTasks(any(), any());
        verify(boardMemberRepository, never()).delete(any(BoardMember.class));
    }

    @Test
    void leaveBoard_whenValidRequestData_shouldRemoveMemberFromBoard() {
        var member = buildBoardMember(ID);

        doReturn(Optional.of(member)).when(boardMemberRepository).findByIdBoardIdAndIdUserId(ID, ID);
        doReturn(3L).when(boardMemberRepository).countByIdBoardIdAndRole(ID, OWNER);

        boardMemberService.leaveBoard(ID, ID);

        verify(boardMemberRepository).findByIdBoardIdAndIdUserId(ID, ID);
        verify(boardMemberRepository).countByIdBoardIdAndRole(ID, OWNER);
        verify(taskRepository).removeAssigneeFromBoardTasks(ID, ID);
        verify(boardMemberRepository).delete(member);
    }

    private BoardMemberResponse buildBoardMemberResponse(Long userId) {
        return new BoardMemberResponse(
                userId, "Dante", "dante@gmail.com", BoardRole.OWNER, LocalDateTime.now());
    }

    private BoardMember buildBoardMember(Long userId) {
        BoardMember member = new BoardMember();
        member.setId(new BoardMemberId(ID, userId));
        member.setRole(OWNER);

        return member;
    }

    private BoardMember buildBoardMember(Board board, User user) {
        BoardMember member = new BoardMember();
        member.setId(new BoardMemberId(board.getId(), user.getId()));
        member.setBoard(board);
        member.setUser(user);
        member.setRole(OWNER);

        return member;
    }

    private CustomUserDetails buildCustomUserDetails() {
        return new CustomUserDetails(ID, "Dante", "dante@gmail.com", "");
    }

    private User getUser() {
        User user = new User();
        user.setId(ID);
        user.setUsername("Re23");
        user.setEmail("re23@gmail.com");

        return user;
    }

    private Board getBoard() {
        Board board = new Board();
        board.setId(ID);
        board.setName("Demo Board");

        return board;
    }

    private BoardMemberResponse buildBoardMemberResponse(User user, BoardRole role) {
        return new BoardMemberResponse(user.getId(), user.getUsername(), user.getEmail(), role, LocalDateTime.now());
    }
}