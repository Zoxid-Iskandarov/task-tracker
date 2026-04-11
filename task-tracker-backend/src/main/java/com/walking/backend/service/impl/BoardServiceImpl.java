package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.board.BoardResponse;
import com.walking.backend.domain.dto.board.BoardRequest;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Board;
import com.walking.backend.repository.BoardRepository;
import com.walking.backend.repository.specification.BoardSpecification;
import com.walking.backend.service.BoardService;
import com.walking.backend.service.UserService;
import com.walking.backend.service.mapper.board.BoardResponseMapper;
import com.walking.backend.service.mapper.board.BoardRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
        Specification<Board> spec = Specification.where(BoardSpecification.hasUserId(userId));

        return boardRepository.findAll(spec, pageable)
                .map(boardResponseMapper::toDto);
    }

    @Override
    public Board getProxyBoardById(Long boardId) {
        return boardRepository.getReferenceById(boardId);
    }

    @Override
    @Transactional
    public BoardResponse createBoard(BoardRequest boardRequest, Long userId) {
        if (boardRepository.existsBoardByNameAndUserId(boardRequest.name(), userId)) {
            throw new DuplicateException("Board with name '%s' already exists".formatted(boardRequest.name()));
        }

        return Optional.of(boardRequest)
                .map(boardRequestMapper::toEntity)
                .map(board -> {
                    board.setUser(userService.getProxyUserById(userId));
                    return boardRepository.save(board);
                })
                .map(boardResponseMapper::toDto)
                .orElseThrow();
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.isOwnerOfBoard(#boardId, #userId)")
    public BoardResponse updateBoard(BoardRequest boardRequest, Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ObjectNotFoundException("Board with id '%d' not found".formatted(boardId)));

        if (boardRepository.existsBoardByNameAndUserIdAndIdNot(boardRequest.name(), userId, board.getId())) {
            throw new DuplicateException("Board with name '%s' already exists".formatted(boardRequest.name()));
        }

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
    @PreAuthorize("@resourceAccessService.isOwnerOfBoard(#boardId, principal.id)")
    public void deleteBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ObjectNotFoundException("Board with id '%d' not found".formatted(boardId)));

        boardRepository.delete(board);
    }
}
