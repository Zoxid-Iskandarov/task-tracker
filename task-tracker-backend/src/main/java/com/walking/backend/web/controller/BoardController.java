package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.board.BoardRequest;
import com.walking.backend.domain.dto.board.BoardResponse;
import com.walking.backend.domain.dto.boardMember.BoardMemberFilter;
import com.walking.backend.domain.dto.boardMember.BoardMemberRequest;
import com.walking.backend.domain.dto.boardMember.BoardMemberResponse;
import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.dto.section.SectionResponse;
import com.walking.backend.domain.dto.task.TaskFilter;
import com.walking.backend.domain.dto.task.TaskPreviewResponse;
import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.security.CustomUserDetails;
import com.walking.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final BoardMemberService boardMemberService;
    private final SectionService sectionService;
    private final TaskService taskService;
    private final LabelService labelService;
    private final UserService userService;

    @GetMapping
    public Page<BoardResponse> getBoards(
            @PageableDefault(30) Pageable pageable,
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
    public Page<TaskPreviewResponse> searchTasks(
            @PathVariable Long boardId,
            TaskFilter taskFilter,
            @PageableDefault(size = 50, sort = "created", direction = Sort.Direction.DESC) Pageable pageable) {
        return taskService.searchTasks(boardId, taskFilter, pageable);
    }

    @PostMapping
    public ResponseEntity<?> createBoard(
            @RequestBody BoardRequest boardRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardService.createBoard(boardRequest, userDetails.id()));
    }

    @PutMapping("/{boardId}")
    public BoardResponse updateBoard(@PathVariable Long boardId, @RequestBody BoardRequest boardRequest) {
        return boardService.updateBoard(boardRequest, boardId);
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<?> deleteBoard(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{boardId}/users/search")
    public Page<UserResponse> searchUsers(
            @PathVariable Long boardId, @RequestParam String query,
            @PageableDefault(50) Pageable pageable) {
        return userService.searchUsersToInvite(boardId, query, pageable);
    }

    @GetMapping("/{boardId}/members")
    public Page<BoardMemberResponse> getMembers(
            @PathVariable Long boardId,
            BoardMemberFilter boardMemberFilter,
            @PageableDefault(size = 50, sort = "joined", direction = Sort.Direction.DESC) Pageable pageable) {
        return boardMemberService.getMembers(boardId, boardMemberFilter, pageable);
    }

    @PostMapping("/{boardId}/members")
    public BoardMemberResponse addMember(
            @PathVariable Long boardId,
            @RequestBody BoardMemberRequest boardMemberRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return boardMemberService.addMember(boardId, boardMemberRequest, userDetails);
    }

    @DeleteMapping("/{boardId}/members/{userId}")
    public ResponseEntity<?> removeMember(
            @PathVariable Long boardId, @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boardMemberService.removeMember(boardId, userId, userDetails.id());

        return ResponseEntity.noContent()
                .build();
    }

    @PatchMapping("/{boardId}/members")
    public BoardMemberResponse changeRole(
            @PathVariable Long boardId,
            @RequestBody BoardMemberRequest boardMemberRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return boardMemberService.changeRole(boardId, boardMemberRequest, userDetails.id());
    }

    @DeleteMapping("/{boardId}/leave")
    public ResponseEntity<?> leaveBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boardMemberService.leaveBoard(boardId, userDetails.id());

        return ResponseEntity.noContent()
                .build();
    }
}
