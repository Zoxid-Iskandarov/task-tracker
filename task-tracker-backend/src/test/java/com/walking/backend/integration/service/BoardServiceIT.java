package com.walking.backend.integration.service;

import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.audit.service.BoardLookupService;
import com.walking.backend.domain.dto.board.BoardRequest;
import com.walking.backend.domain.dto.board.BoardResponse;
import com.walking.backend.domain.model.Board;
import com.walking.backend.domain.model.BoardMember;
import com.walking.backend.domain.projection.BoardInfo;
import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.integration.annotation.WithMockUser;
import com.walking.backend.repository.BoardMemberRepository;
import com.walking.backend.repository.BoardRepository;
import com.walking.backend.service.BoardService;
import com.walking.backend.storage.service.ResourceCleanupService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static com.walking.backend.domain.model.ActivityType.BOARD_DELETED;
import static com.walking.backend.domain.model.ActivityType.BOARD_UPDATED;
import static com.walking.backend.domain.model.BoardRole.OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@WithMockUser
@RequiredArgsConstructor
public class BoardServiceIT extends IntegrationTestBase {
    private final BoardService boardService;
    private final BoardLookupService boardLookupService;
    private final ActivityService activityService;
    private final ResourceCleanupService resourceCleanupService;

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;

    @Test
    void getBoards_whenUserHasBoards_shouldReturnOnlyUserBoards() {
        var pageable = PageRequest.of(0, 10);

        Page<BoardResponse> actual = boardService.getBoards(2L, pageable);

        assertThat(actual.getContent()).isNotEmpty();
        assertThat(actual.getContent()).hasSize(2);
        assertThat(actual.getContent())
                .extracting(BoardResponse::name)
                .contains("Test Board", "Second Test Board");
    }

    @Test
    void getBoards_whenUserIsMemberOfOneBoard_shouldNotReturnOtherBoards() {
        var pageable = PageRequest.of(0, 10);

        Page<BoardResponse> actual = boardService.getBoards(1L, pageable);

        assertThat(actual.getContent()).isNotEmpty();
        assertThat(actual.getContent()).hasSize(1);
        assertThat(actual.getContent())
                .extracting(BoardResponse::name)
                .contains("Second Test Board");
    }

    @Test
    void createBoard_whenValidRequestData_shouldCreateBoardWithOwner() {
        Long userId = 1L;
        var boardRequest = new BoardRequest("New Board");

        BoardResponse actual = boardService.createBoard(boardRequest, userId);

        assertThat(actual).isNotNull();
        assertThat(actual.name()).isEqualTo(boardRequest.name());

        Optional<Board> boardOptional = boardRepository.findById(actual.id());
        assertThat(boardOptional).isPresent();
        assertThat(boardOptional.get().getName()).isEqualTo(boardRequest.name());

        Optional<BoardMember> memberOptional = boardMemberRepository.findByIdBoardIdAndIdUserId(actual.id(), userId);
        assertThat(memberOptional).isPresent();
        assertThat(memberOptional.get().getRole()).isEqualTo(OWNER);
    }

    @Test
    void updateBoard_whenOwnerUpdates_shouldUpdateBoard() {
        Long boardId = 1L;
        var boardRequest = new BoardRequest("Renamed Board");

        BoardResponse actual = boardService.updateBoard(boardRequest, boardId);

        assertThat(actual).isNotNull();
        assertThat(actual.name()).isEqualTo(boardRequest.name());

        Optional<Board> boardOptional = boardRepository.findById(boardId);

        assertThat(boardOptional).isPresent();
        assertThat(boardOptional.get().getName()).isEqualTo(boardRequest.name());

        verify(activityService).publish(eq(boardOptional.get()), eq(BOARD_UPDATED), anyString());
    }

    @Test
    void updateBoard_whenViewerTriesToUpdate_shouldThrowAccessDenied() {
        var boardRequest = new BoardRequest("Hacked Name");

        assertThatThrownBy(() -> boardService.updateBoard(boardRequest, 2L))
                .isInstanceOf(AccessDeniedException.class);

        Board unchanged = boardRepository.findById(2L).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Second Test Board");
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe")
    void updateBoard_whenNonMemberTriesToUpdate_shouldThrowAccessDenied() {
        var boardRequest = new BoardRequest("Hacked Name");

        assertThatThrownBy(() -> boardService.updateBoard(boardRequest, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateBoard_whenRenamed_shouldPublishActivity() {
        Long boardId = 1L;
        var boardRequest = new BoardRequest("New Name For Activity");

        boardService.updateBoard(boardRequest, boardId);

        Board board = boardRepository.findById(boardId).orElseThrow();

        verify(activityService).publish(board, BOARD_UPDATED,
                "Renamed board from Test Board to New Name For Activity");
    }

    @Test
    void updateBoard_whenCalled_shouldEvictBoardCaches() {
        boardLookupService.getBoardInfoById(1L);

        var boardRequest = new BoardRequest("Cache Evict Name");
        boardService.updateBoard(boardRequest, 1L);

        boardRepository.findById(1L)
                .ifPresent(b -> {
                    b.setName("DB DIRECT CHANGE");
                    boardRepository.save(b);
                });

        BoardInfo refreshed = boardLookupService.getBoardInfoById(1L);
        assertThat(refreshed.name()).isEqualTo("DB DIRECT CHANGE");
    }

    @Test
    void deleteBoard_whenOwnerDeletes_shouldRemoveFromDb() {
        boardService.deleteBoard(1L);

        assertThat(boardRepository.findById(1L)).isEmpty();

        verify(activityService).publish(any(Board.class), eq(BOARD_DELETED), eq("Deleted board Test Board"));
        verify(resourceCleanupService).cleanupFiles(anyList());
    }

    @Test
    void deleteBoard_whenViewerTriesToDelete_shouldThrowAccessDenied() {
        assertThatThrownBy(() -> boardService.deleteBoard(2L))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(boardRepository.findById(2L)).isPresent();
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe")
    void deleteBoard_whenNonMemberTriesToDelete_shouldThrowAccessDenied() {
        assertThatThrownBy(() -> boardService.deleteBoard(1L))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(boardRepository.findById(1L)).isPresent();
    }

    @Test
    void deleteBoard_whenCalled_shouldEvictBoardCaches() {
        boardLookupService.getBoardInfoById(1L);

        boardService.deleteBoard(1L);

        BoardInfo actual = boardLookupService.getBoardInfoById(1L);

        assertThat(actual).isNotNull();
        assertThat(actual.name()).isEqualTo("Unknown Board");
    }
}
