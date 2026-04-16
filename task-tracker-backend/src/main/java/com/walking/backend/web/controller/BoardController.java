package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.board.BoardRequest;
import com.walking.backend.domain.dto.board.BoardResponse;
import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.dto.section.SectionResponse;
import com.walking.backend.domain.dto.task.TaskFilter;
import com.walking.backend.domain.dto.task.TaskResponse;
import com.walking.backend.security.CustomUserDetails;
import com.walking.backend.service.BoardService;
import com.walking.backend.service.LabelService;
import com.walking.backend.service.SectionService;
import com.walking.backend.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;
    private final SectionService sectionService;
    private final TaskService taskService;
    private final LabelService labelService;

    @GetMapping
    public Page<BoardResponse> getBoards(@PageableDefault(30) Pageable pageable,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        return boardService.getBoards(userDetails.id(), pageable);
    }

    @GetMapping("/{boardId}/sections")
    public Page<SectionResponse> getSections(@PathVariable Long boardId, @PageableDefault(30) Pageable pageable) {
        return sectionService.getSections(boardId, pageable);
    }

    @GetMapping("/{boardId}/labels")
    public List<LabelResponse> getLabels(@PathVariable Long boardId, @RequestParam(required = false) String name) {
        return labelService.getLabels(boardId, name);
    }

    @GetMapping("/{boardId}/tasks")
    public Page<TaskResponse> searchTasks(@PathVariable Long boardId,
                                          TaskFilter taskFilter,
                                          @PageableDefault(50) Pageable pageable) {
        return taskService.searchTasks(boardId, taskFilter, pageable);
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
    public ResponseEntity<?> deleteBoard(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);

        return ResponseEntity.noContent().build();
    }
}
