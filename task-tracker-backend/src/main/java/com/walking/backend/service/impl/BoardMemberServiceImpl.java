package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.boardMember.BoardMemberRequest;
import com.walking.backend.domain.dto.boardMember.BoardMemberResponse;
import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.IllegalOperationException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Board;
import com.walking.backend.domain.model.BoardMember;
import com.walking.backend.domain.model.User;
import com.walking.backend.repository.BoardMemberRepository;
import com.walking.backend.security.CustomUserDetails;
import com.walking.backend.service.BoardMemberService;
import com.walking.backend.service.BoardService;
import com.walking.backend.service.KafkaProducerService;
import com.walking.backend.service.UserService;
import com.walking.backend.service.mapper.boardMember.BoardMemberResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public BoardMember getById(Long boardId, Long userId) {
        return boardMemberRepository.findByIdBoardIdAndIdUserId(boardId, userId)
                .orElseThrow(() -> new ObjectNotFoundException("Member with id '%d' in board with id '%d' not found"
                        .formatted(userId, boardId)));
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageBoard(#boardId, #userDetails.id())")
    public BoardMemberResponse addMember(
            Long boardId, BoardMemberRequest boardMemberRequest,
            CustomUserDetails userDetails) {
        if (boardMemberRepository.existsByIdBoardIdAndIdUserId(boardId, boardMemberRequest.userId())) {
            throw new DuplicateException("User with id '%d' already exists in board members"
                    .formatted(boardMemberRequest.userId()));
        }

        Board board = boardService.getProxyBoardById(boardId);
        User user = userService.getUserById(boardMemberRequest.userId());

        BoardMember newMember = new BoardMember(board, user, boardMemberRequest.role());
        board.getMembers().add(newMember);
        boardMemberRepository.flush();

        kafkaProducerService.sendMessageDto(user.getId().toString(),
                createBoardInvitationMessage(user, board.getName(), userDetails.username()));

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
        boardMember.setRole(boardMemberRequest.role());
        boardMemberRepository.flush();

        return boardMemberResponseMapper.toDto(boardMember);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canViewBoard(#boardId, #currentUserId)")
    public void leaveBoard(Long boardId, Long currentUserId) {
        BoardMember member = getById(boardId, currentUserId);

        if (member.getRole() == OWNER && boardMemberRepository.countByIdBoardIdAndRole(boardId, OWNER) <= 1) {
            throw new IllegalOperationException("Last owner cannot leave the board");
        }

        boardMemberRepository.delete(member);
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
