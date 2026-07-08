package com.walking.backend.service;

import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.board.BoardRequest;
import com.walking.backend.domain.dto.board.BoardResponse;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.*;
import com.walking.backend.repository.BoardRepository;
import com.walking.backend.repository.TaskAttachmentRepository;
import com.walking.backend.service.impl.BoardServiceImpl;
import com.walking.backend.service.mapper.board.BoardRequestMapper;
import com.walking.backend.service.mapper.board.BoardResponseMapper;
import com.walking.backend.storage.service.ResourceCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.walking.backend.domain.model.ActivityType.BOARD_DELETED;
import static com.walking.backend.domain.model.ActivityType.BOARD_UPDATED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {
    private static final Long ID = 1L;
    private static final String USERNAME = "Dante";

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private TaskAttachmentRepository taskAttachmentRepository;

    @Mock
    private UserService userService;

    @Mock
    private ActivityService activityService;

    @Mock
    private ResourceCleanupService resourceCleanupService;

    @Mock
    private BoardRequestMapper boardRequestMapper;

    @Mock
    private BoardResponseMapper boardResponseMapper;

    @InjectMocks
    private BoardServiceImpl boardService;

    @Test
    void getBoards_whenValidRequestData_shouldReturnPageOfBoardResponses() {
        var board1 = buildBoard(1L, "Board One");
        var board2 = buildBoard(2L, "Board Two");

        var pageable = PageRequest.of(0, 10);
        var boardResponses = new PageImpl<>(List.of(board1, board2), pageable, 2L);

        var boardResponse1 = buildBoardResponse(1L, "Board One");
        var boardResponse2 = buildBoardResponse(2L, "Board Two");

        doReturn(boardResponses).when(boardRepository).findAllByUserId(ID, pageable);
        doReturn(boardResponse1).when(boardResponseMapper).toDto(board1);
        doReturn(boardResponse2).when(boardResponseMapper).toDto(board2);

        Page<BoardResponse> actual = boardService.getBoards(ID, pageable);

        assertTrue(actual.hasContent());
        assertEquals(2, actual.getContent().size());
        assertEquals(boardResponse1, actual.getContent().get(0));
        assertEquals(boardResponse2, actual.getContent().get(1));

        verify(boardRepository).findAllByUserId(ID, pageable);
        verify(boardResponseMapper).toDto(board1);
        verify(boardResponseMapper).toDto(board2);
    }

    @Test
    void createBoard_whenValidRequestData_shouldCreateBoardAndReturnBoardResponse() {
        var boardRequest = getBoardRequest();
        var board = buildBoard(ID, boardRequest.name());
        var boardResponse = buildBoardResponse(ID, boardRequest.name());
        var user = getUser();

        doReturn(board).when(boardRequestMapper).toEntity(boardRequest);
        doReturn(user).when(userService).getProxyUserById(ID);
        doReturn(board).when(boardRepository).save(board);
        doReturn(boardResponse).when(boardResponseMapper).toDto(board);

        BoardResponse actual = boardService.createBoard(boardRequest, ID);

        assertEquals(boardResponse, actual);

        assertEquals(1, board.getMembers().size());

        for (BoardMember member : board.getMembers()) {
            assertEquals(user, member.getUser());
            assertEquals(board, member.getBoard());
            assertEquals(BoardRole.OWNER, member.getRole());
        }

        verify(boardRequestMapper).toEntity(boardRequest);
        verify(userService).getProxyUserById(ID);
        verify(boardRepository).save(board);
        verify(boardResponseMapper).toDto(board);
    }

    @Test
    void updateBoard_whenValidRequestData_shouldUpdateBoardAndReturnBoardResponse() {
        var boardRequest = getBoardRequest();
        var board = buildBoard(ID, "Board RE4");
        var boardResponse = buildBoardResponse(ID, boardRequest.name());

        doReturn(Optional.of(board)).when(boardRepository).findById(ID);
        doReturn(board).when(boardRepository).save(board);
        doReturn(boardResponse).when(boardResponseMapper).toDto(board);

        BoardResponse actual = boardService.updateBoard(boardRequest, ID);

        assertEquals(boardResponse, actual);
        assertEquals(boardRequest.name(), board.getName());

        verify(boardRepository).findById(ID);
        verify(boardRepository).save(board);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(activityService).publish(eq(board), eq(BOARD_UPDATED), captor.capture());

        assertEquals("Renamed board from Board RE4 to %s".formatted(boardRequest.name()), captor.getValue());

        verify(boardResponseMapper).toDto(board);
    }

    @Test
    void updateBoard_whenBoardNotFound_shouldThrowObjectNotFoundException() {
        var boardRequest = getBoardRequest();

        doReturn(Optional.empty()).when(boardRepository).findById(ID);

        assertThrows(ObjectNotFoundException.class, () -> boardService.updateBoard(boardRequest, ID));

        verify(boardRepository).findById(ID);

        verify(boardRepository).findById(ID);
        verify(boardRepository, never()).save(any());
        verify(activityService, never()).publish(any(), any(), anyString());
        verify(boardResponseMapper, never()).toDto(any());
    }

    @Test
    void deleteBoard_whenValidRequestData_shouldDeleteBoard() {
        var board = buildBoard(ID, "Board RE4");
        var filePaths = List.of("path/to/file1.png", "path/to/file2.png");

        doReturn(Optional.of(board)).when(boardRepository).findById(ID);
        doReturn(filePaths).when(taskAttachmentRepository).findAllFilePathByBoardId(ID);

        boardService.deleteBoard(ID);

        verify(boardRepository).findById(ID);
        verify(taskAttachmentRepository).findAllFilePathByBoardId(ID);
        verify(boardRepository).delete(board);
        verify(activityService).publish(board, BOARD_DELETED, "Deleted board %s".formatted(board.getName()));
        verify(resourceCleanupService).cleanupFiles(filePaths);
    }

    @Test
    void deleteBoard_whenBoardNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(boardRepository).findById(ID);

        assertThrows(ObjectNotFoundException.class, () -> boardService.deleteBoard(ID));

        verify(boardRepository).findById(ID);
        verify(taskAttachmentRepository, never()).findAllFilePathByBoardId(anyLong());
        verify(boardRepository, never()).delete(any());
        verify(activityService, never()).publish(any(), any(), anyString());
        verify(resourceCleanupService, never()).cleanupFiles(anyList());
    }

    private BoardResponse buildBoardResponse(Long id, String name) {
        return new BoardResponse(id, name, LocalDateTime.now(), LocalDateTime.now());
    }

    private Board buildBoard(Long id, String name) {
        Board board = new Board();
        board.setId(id);
        board.setName(name);
        return board;
    }

    private BoardRequest getBoardRequest() {
        return new BoardRequest("Board RE7");
    }

    private User getUser() {
        User user = new User();
        user.setId(ID);
        user.setUsername(USERNAME);
        return user;
    }
}