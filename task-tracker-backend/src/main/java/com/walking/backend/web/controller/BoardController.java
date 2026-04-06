package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.board.BoardRequest;
import com.walking.backend.domain.dto.board.BoardResponse;
import com.walking.backend.security.CustomUserDetails;
import com.walking.backend.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;

    @GetMapping
    public Page<BoardResponse> getBoards(@PageableDefault(30) Pageable pageable,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        return boardService.getBoards(userDetails.id(), pageable);
    }

    @PostMapping
    public ResponseEntity<?> createBoard(@RequestBody BoardRequest boardRequest,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardService.createBoard(boardRequest, userDetails.id()));
    }

    @PutMapping("/{boardId}")
    public BoardResponse updateBoard(@PathVariable Long boardId,
                                     @RequestBody BoardRequest boardRequest,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        return boardService.updateBoard(boardRequest, boardId, userDetails.id());
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<?> deleteBord(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);

        return ResponseEntity.noContent().build();
    }
}
