package com.walking.backend.service.impl;

import com.walking.backend.audit.annotation.TrackActivity;
import com.walking.backend.domain.dto.activity.UserActivityInternalEvent;
import com.walking.backend.domain.dto.boardMember.BoardMemberFilter;
import com.walking.backend.domain.dto.boardMember.BoardMemberRequest;
import com.walking.backend.domain.dto.boardMember.BoardMemberResponse;
import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.IllegalOperationException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.*;
import com.walking.backend.repository.BoardMemberRepository;
import com.walking.backend.repository.specification.BoardMemberSpecification;
import com.walking.backend.security.principal.CustomUserDetails;
import com.walking.backend.service.BoardMemberService;
import com.walking.backend.service.BoardService;
import com.walking.backend.service.KafkaProducerService;
import com.walking.backend.service.UserService;
import com.walking.backend.service.mapper.boardMember.BoardMemberResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.walking.backend.domain.model.ActivityType.*;
import static com.walking.backend.domain.model.BoardRole.OWNER;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardMemberServiceImpl implements BoardMemberService {
    private final BoardMemberRepository boardMemberRepository;
    private final BoardService boardService;
    private final UserService userService;
    private final BoardMemberResponseMapper boardMemberResponseMapper;
    private final KafkaProducerService kafkaProducerService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @PreAuthorize("@resourceAccessService.canViewBoard(#boardId, principal.id)")
    public Page<BoardMemberResponse> getMembers(Long boardId, BoardMemberFilter boardMemberFilter, Pageable pageable) {
        Specification<BoardMember> spec = BoardMemberSpecification.hasBoardId(boardId)
                .and(BoardMemberSpecification.hasUsername(boardMemberFilter.username()))
                .and(BoardMemberSpecification.hasEmail(boardMemberFilter.email()))
                .and(BoardMemberSpecification.hasRole(boardMemberFilter.role()))
                .and(BoardMemberSpecification.hasJoinedBetween(
                        boardMemberFilter.joinedFrom(), boardMemberFilter.joinedTo()));

        return boardMemberRepository.findAll(spec, pageable)
                .map(boardMemberResponseMapper::toDto);
    }

    @Override
    public BoardMember getById(Long boardId, Long userId) {
        return boardMemberRepository.findByIdBoardIdAndIdUserId(boardId, userId)
                .orElseThrow(() -> new ObjectNotFoundException("Member with id %d in board with id %d not found"
                        .formatted(userId, boardId)));
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageBoard(#boardId, #userDetails.id())")
    @TrackActivity(type = MEMBER_ADDED, description = "'Added member ' + #result.username")
    public BoardMemberResponse addMember(
            Long boardId,
            BoardMemberRequest boardMemberRequest,
            CustomUserDetails userDetails) {
        if (boardMemberRepository.existsByIdBoardIdAndIdUserId(boardId, boardMemberRequest.userId())) {
            throw new DuplicateException("User with id %d is already a member of this board"
                    .formatted(boardMemberRequest.userId()));
        }

        Board board = boardService.getProxyBoardById(boardId);
        User user = userService.getUserById(boardMemberRequest.userId());

        BoardMember newMember = new BoardMember(board, user, boardMemberRequest.role());
        board.getMembers().add(newMember);
        boardMemberRepository.flush();

        kafkaProducerService.sendMessageDto(user.getId(), createBoardInvitationMessage(
                user, board.getName(), userDetails.username()));

        return boardMemberResponseMapper.toDto(newMember);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageBoard(#boardId, #currentUserId)")
    public void removeMember(Long boardId, Long userId, Long currentUserId) {
        if (currentUserId.equals(userId)) {
            throw new IllegalOperationException("You cannot remove yourself from the board");
        }

        BoardMember boardMember = getById(boardId, userId);

        publishActivity(boardId, boardMember.getBoard().getName(),
                MEMBER_REMOVED, "Removed member %s".formatted(boardMember.getUser().getUsername()));

        boardMemberRepository.delete(boardMember);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageBoard(#boardId, #currentUserId)")
    public BoardMemberResponse changeRole(Long boardId, BoardMemberRequest boardMemberRequest, Long currentUserId) {
        if (currentUserId.equals(boardMemberRequest.userId())) {
            throw new IllegalOperationException("You cannot change your own role");
        }

        BoardMember boardMember = getById(boardId, boardMemberRequest.userId());

        String oldRole = boardMember.getRole().name();
        String newRole = boardMemberRequest.role().name();

        boardMember.setRole(boardMemberRequest.role());
        boardMemberRepository.flush();

        publishActivity(boardId, boardMember.getBoard().getName(), MEMBER_ROLE_CHANGED,
                "Changed role for %s from %s to %s".formatted(boardMember.getUser().getUsername(), oldRole, newRole));

        return boardMemberResponseMapper.toDto(boardMember);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canViewBoard(#boardId, #currentUserId)")
    @TrackActivity(type = MEMBER_LEAVED, description = "'Left the board'")
    public void leaveBoard(Long boardId, Long currentUserId) {
        BoardMember member = getById(boardId, currentUserId);

        if (member.getRole() == OWNER && boardMemberRepository.countByIdBoardIdAndRole(boardId, OWNER) <= 1) {
            throw new IllegalOperationException("Last owner cannot leave the board");
        }

        boardMemberRepository.delete(member);
    }

    private void publishActivity(Long boardId, String boardName, ActivityType type, String description) {
        CustomUserDetails userDetails = getCurrentUser();

        applicationEventPublisher.publishEvent(new UserActivityInternalEvent(
                userDetails.id(),
                userDetails.username(),
                userDetails.email(),
                boardId,
                boardName,
                type,
                description));
    }

    private CustomUserDetails getCurrentUser() {
        return (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private MessageDto createBoardInvitationMessage(User user, String boardName, String inviterName) {
        String message = """
                Hello, %s!
                
                You have been added to the board "%s" in Task Tracker.
                
                Added by: %s
                
                You can now access the board, view tasks, and collaborate depending on your role.
                
                Open Task Tracker to get started.
                
                Best regards,
                Task Tracker Team
                """.formatted(user.getUsername(), boardName, inviterName);

        return new MessageDto(user.getEmail(), "You've been added to a board", message);
    }
}
