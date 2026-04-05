package com.walking.backend.service;

import com.walking.backend.domain.dto.board.BoardRequest;
import com.walking.backend.domain.dto.board.BoardResponse;
import com.walking.backend.domain.model.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardService {

    Page<BoardResponse> getBoards(Long userId, Pageable pageable);

    Board getBoardById(Long boardId);

    BoardResponse createBoard(BoardRequest boardRequest, Long userId);

    BoardResponse updateBoard(BoardRequest boardRequest, Long boarId, Long userId);

    void deleteBoard(Long boardId);
}
