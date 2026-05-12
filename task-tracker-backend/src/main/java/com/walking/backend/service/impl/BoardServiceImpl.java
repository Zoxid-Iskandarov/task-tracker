package com.walking.backend.service.impl;

import com.walking.backend.audit.annotation.TrackActivity;
import com.walking.backend.domain.dto.activity.UserActivityInternalEvent;
import com.walking.backend.domain.dto.board.BoardRequest;
import com.walking.backend.domain.dto.board.BoardResponse;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.*;
import com.walking.backend.repository.BoardRepository;
import com.walking.backend.security.principal.CustomUserDetails;
import com.walking.backend.service.BoardService;
import com.walking.backend.service.UserService;
import com.walking.backend.service.mapper.board.BoardRequestMapper;
import com.walking.backend.service.mapper.board.BoardResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.walking.backend.domain.model.ActivityType.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {
    private final UserService userService;
    private final BoardRepository boardRepository;
    private final BoardRequestMapper boardRequestMapper;
    private final BoardResponseMapper boardResponseMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Page<BoardResponse> getBoards(Long userId, Pageable pageable) {
        return boardRepository.findAllByUserId(userId, pageable)
                .map(boardResponseMapper::toDto);
    }

    @Override
    public Board getProxyBoardById(Long boardId) {
        return boardRepository.getReferenceById(boardId);
    }

    @Override
    @Transactional
    @TrackActivity(type = BOARD_CREATED, description = "'Created board ' + #result.name")
    public BoardResponse createBoard(BoardRequest boardRequest, Long userId) {
        Board board = boardRequestMapper.toEntity(boardRequest);
        User user = userService.getProxyUserById(userId);

        BoardMember member = new BoardMember(board, user, BoardRole.OWNER);
        board.getMembers().add(member);

        Board savedBoard = boardRepository.save(board);

        return boardResponseMapper.toDto(savedBoard);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageBoard(#boardId, principal.id)")
    public BoardResponse updateBoard(BoardRequest boardRequest, Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ObjectNotFoundException("Board with id '%d' not found".formatted(boardId)));

        String oldName = board.getName();
        String newName = boardRequest.name();

        board.setName(newName);

        Board updatedBoard = boardRepository.save(board);

        publishActivity(boardId, newName, BOARD_UPDATED, "Updated board from '%s' to '%s'".formatted(oldName, newName));

        return boardResponseMapper.toDto(updatedBoard);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageBoard(#boardId, principal.id)")
    public void deleteBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ObjectNotFoundException("Board with id '%d' not found".formatted(boardId)));

        String boardName = board.getName();

        publishActivity(boardId, boardName, BOARD_DELETED, "Deleted board '%s'".formatted(boardName));

        boardRepository.delete(board);
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
}
