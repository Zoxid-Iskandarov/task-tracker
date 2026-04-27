package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.board.BoardRequest;
import com.walking.backend.domain.dto.board.BoardResponse;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.*;
import com.walking.backend.repository.BoardRepository;
import com.walking.backend.service.BoardService;
import com.walking.backend.service.UserService;
import com.walking.backend.service.mapper.board.BoardRequestMapper;
import com.walking.backend.service.mapper.board.BoardResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {
    private final UserService userService;
    private final BoardRepository boardRepository;
    private final BoardRequestMapper boardRequestMapper;
    private final BoardResponseMapper boardResponseMapper;

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

        return Optional.of(board)
                .map(boardEntity -> {
                    boardEntity.setName(boardRequest.name());
                    return boardRepository.save(board);
                })
                .map(boardResponseMapper::toDto)
                .orElseThrow();
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageBoard(#boardId, principal.id)")
    public void deleteBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ObjectNotFoundException("Board with id '%d' not found".formatted(boardId)));

        boardRepository.delete(board);
    }
}
