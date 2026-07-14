package com.walking.backend.service;

import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.label.CreateLabelRequest;
import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.dto.label.UpdateLabelRequest;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.LabelLimitExceededException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Board;
import com.walking.backend.domain.model.Label;
import com.walking.backend.props.AppProperties;
import com.walking.backend.repository.LabelRepository;
import com.walking.backend.service.impl.LabelServiceImpl;
import com.walking.backend.service.mapper.label.CreateLabelRequestMapper;
import com.walking.backend.service.mapper.label.LabelResponseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.walking.backend.domain.model.ActivityType.LABEL_DELETED;
import static com.walking.backend.domain.model.ActivityType.LABEL_UPDATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {
    private static final Long LABEL_ID = 1L;
    private static final Long BOARD_ID = 2L;
    private static final String LABEL_NAME = "Bug";
    private static final String LABEL_COLOUR = "#A1B2C3";
    
    @Mock
    private BoardService boardService;
    
    @Mock
    private LabelRepository labelRepository;
    
    @Mock
    private ActivityService activityService;
    
    @Mock
    private CreateLabelRequestMapper createLabelRequestMapper;
    
    @Mock
    private LabelResponseMapper labelResponseMapper;
    
    @Mock
    private AppProperties appProperties;
    
    @InjectMocks
    private LabelServiceImpl labelService;

    @Test
    void getLabels_whenNameProvided_shouldReturnFilteredLabels() {
        var label1 = buildLabel(1L, "Bug Fix");
        var label2 = buildLabel(2L, "Bug Report");
        var labelResponse1 = buildLabelResponse(1L, "Bug Fix");
        var labelResponse2 = buildLabelResponse(2L, "Bug Report");

        doReturn(List.of(label1, label2)).when(labelRepository)
                .findAllByBoardIdAndNameContainingIgnoreCase(BOARD_ID, LABEL_NAME);
        doReturn(labelResponse1).when(labelResponseMapper).toDto(label1);
        doReturn(labelResponse2).when(labelResponseMapper).toDto(label2);

        List<LabelResponse> actual = labelService.getLabels(BOARD_ID, LABEL_NAME);

        assertEquals(List.of(labelResponse1, labelResponse2), actual);

        verify(labelRepository, never()).findAllByBoardId(anyLong());
        verify(labelRepository).findAllByBoardIdAndNameContainingIgnoreCase(BOARD_ID, LABEL_NAME);
        verify(labelResponseMapper, times(2)).toDto(any(Label.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void getLabels_whenNameIsNullOrBlank_shouldReturnAllLabelsForBoard(String name) {
        var label1 = buildLabel(1L, "Bug Fix");
        var label2 = buildLabel(2L, "Bug Report");
        var labelResponse1 = buildLabelResponse(1L, "Bug Fix");
        var labelResponse2 = buildLabelResponse(2L, "Bug Report");

        doReturn(List.of(label1, label2)).when(labelRepository).findAllByBoardId(BOARD_ID);
        doReturn(labelResponse1).when(labelResponseMapper).toDto(label1);
        doReturn(labelResponse2).when(labelResponseMapper).toDto(label2);

        List<LabelResponse> actual = labelService.getLabels(BOARD_ID, name);

        assertEquals(List.of(labelResponse1, labelResponse2), actual);

        verify(labelRepository).findAllByBoardId(anyLong());
        verify(labelRepository, never()).findAllByBoardIdAndNameContainingIgnoreCase(anyLong(), anyString());
        verify(labelResponseMapper, times(2)).toDto(any(Label.class));
    }

    @Test
    void createLabel_whenLabelLimitExceeded_shouldThrowLabelLimitExceededException() {
        var labelProperties = mock(AppProperties.Label.class);
        var createLabelRequest = new CreateLabelRequest(LABEL_NAME, LABEL_COLOUR, BOARD_ID);

        doReturn(labelProperties).when(appProperties).getLabel();
        doReturn(10).when(labelProperties).getMaxPerBoard();
        doReturn(10L).when(labelRepository).countByBoardId(createLabelRequest.boardId());

        assertThrows(LabelLimitExceededException.class, () -> labelService.createLabel(createLabelRequest));

        verify(labelRepository).countByBoardId(createLabelRequest.boardId());
        verify(labelRepository, never()).existsByNameAndBoardId(anyString(), anyLong());
        verify(labelRepository, never()).save(any(Label.class));
        verifyNoInteractions(createLabelRequestMapper, boardService, labelResponseMapper);
    }

    @Test
    void createLabel_whenLabelNameAlreadyExists_shouldThrowDuplicateException() {
        var labelProperties = mock(AppProperties.Label.class);
        var createLabelRequest = new CreateLabelRequest(LABEL_NAME, LABEL_COLOUR, BOARD_ID);

        doReturn(labelProperties).when(appProperties).getLabel();
        doReturn(10).when(labelProperties).getMaxPerBoard();
        doReturn(5L).when(labelRepository).countByBoardId(createLabelRequest.boardId());
        doReturn(true).when(labelRepository)
                .existsByNameAndBoardId(createLabelRequest.name(), createLabelRequest.boardId());

        assertThrows(DuplicateException.class, () -> labelService.createLabel(createLabelRequest));

        verify(labelRepository).countByBoardId(createLabelRequest.boardId());
        verify(labelRepository).existsByNameAndBoardId(createLabelRequest.name(), createLabelRequest.boardId());
        verify(labelRepository, never()).save(any(Label.class));
        verifyNoInteractions(createLabelRequestMapper, boardService, labelResponseMapper);
    }

    @Test
    void createLabel_whenValidRequestData_shouldCreateLabelAndReturnLabelResponse() {
        var labelProperties = mock(AppProperties.Label.class);
        var createLabelRequest = new CreateLabelRequest(LABEL_NAME, LABEL_COLOUR, BOARD_ID);
        var label = buildLabel(LABEL_ID, LABEL_NAME);
        var board = label.getBoard();
        var labelResponse = buildLabelResponse(LABEL_ID, LABEL_NAME);

        doReturn(labelProperties).when(appProperties).getLabel();
        doReturn(10).when(labelProperties).getMaxPerBoard();
        doReturn(5L).when(labelRepository).countByBoardId(createLabelRequest.boardId());
        doReturn(false).when(labelRepository)
                .existsByNameAndBoardId(createLabelRequest.name(), createLabelRequest.boardId());
        doReturn(label).when(createLabelRequestMapper).toEntity(createLabelRequest);
        doReturn(board).when(boardService).getProxyBoardById(createLabelRequest.boardId());
        doReturn(label).when(labelRepository).save(label);
        doReturn(labelResponse).when(labelResponseMapper).toDto(label);

        LabelResponse actual = labelService.createLabel(createLabelRequest);

        assertEquals(labelResponse, actual);
        assertEquals(board.getId(), actual.boardId());

        verify(labelRepository).countByBoardId(createLabelRequest.boardId());
        verify(labelRepository).existsByNameAndBoardId(createLabelRequest.name(), createLabelRequest.boardId());
        verify(createLabelRequestMapper).toEntity(createLabelRequest);
        verify(boardService).getProxyBoardById(createLabelRequest.boardId());
        verify(labelRepository).save(label);
        verify(labelResponseMapper).toDto(label);
    }

    @Test
    void updateLabel_whenLabelNotFound_shouldThrowObjectNotFoundException() {
        var updateLabelRequest = new UpdateLabelRequest("Fix", "Red");

        doReturn(Optional.empty()).when(labelRepository).findById(LABEL_ID);

        assertThrows(ObjectNotFoundException.class, () -> labelService.updateLabel(updateLabelRequest, LABEL_ID));

        verify(labelRepository).findById(LABEL_ID);
        verify(labelRepository, never()).existsByNameAndBoardIdAndIdNot(anyString(), anyLong(), anyLong());
        verify(labelRepository, never()).save(any(Label.class));
        verifyNoInteractions(activityService, labelResponseMapper);
    }

    @Test
    void updateLabel_whenLabelByNameAlreadyExistsInBoard_shouldThrowDuplicateException() {
        var updateLabelRequest = new UpdateLabelRequest("Fix", "Red");
        var label = buildLabel(LABEL_ID, LABEL_NAME);
        var board = label.getBoard();

        doReturn(Optional.of(label)).when(labelRepository).findById(label.getId());
        doReturn(true).when(labelRepository)
                .existsByNameAndBoardIdAndIdNot(updateLabelRequest.name(), board.getId(), label.getId());

        assertThrows(DuplicateException.class, () -> labelService.updateLabel(updateLabelRequest, label.getId()));

        verify(labelRepository).findById(label.getId());
        verify(labelRepository).existsByNameAndBoardIdAndIdNot(updateLabelRequest.name(), board.getId(), label.getId());
        verify(labelRepository, never()).save(any(Label.class));
        verifyNoInteractions(activityService, labelResponseMapper);
    }

    @Test
    void updateLabel_whenNameChanged_shouldUpdateLabelAndPublishRenamedActivity() {
        var updateLabelRequest = new UpdateLabelRequest("Fix", "Blue");
        var label = buildLabel(LABEL_ID, LABEL_NAME);
        var board = label.getBoard();
        var labelResponse = buildLabelResponse(LABEL_ID, updateLabelRequest.name());

        doReturn(Optional.of(label)).when(labelRepository).findById(label.getId());
        doReturn(false).when(labelRepository)
                .existsByNameAndBoardIdAndIdNot(updateLabelRequest.name(), board.getId(), label.getId());
        doReturn(label).when(labelRepository).save(label);
        doReturn(labelResponse).when(labelResponseMapper).toDto(label);

        LabelResponse actual = labelService.updateLabel(updateLabelRequest, label.getId());

        assertEquals(labelResponse, actual);
        assertEquals(updateLabelRequest.name(), label.getName());
        assertEquals(updateLabelRequest.colour(), label.getColour());

        verify(labelRepository).findById(label.getId());
        verify(labelRepository).existsByNameAndBoardIdAndIdNot(updateLabelRequest.name(), board.getId(), label.getId());
        verify(labelRepository).save(label);
        verify(activityService).publish(board, LABEL_UPDATED,
                "Renamed label from %s to %s".formatted(LABEL_NAME, updateLabelRequest.name()));
        verify(labelResponseMapper).toDto(label);
    }

    @Test
    void updateLabel_whenNameUnchanged_shouldUpdateLabelAndPublishUpdatedActivity() {
        var updateLabelRequest = new UpdateLabelRequest(LABEL_NAME, "Blue");
        var label = buildLabel(LABEL_ID, LABEL_NAME);
        var board = label.getBoard();
        var labelResponse = buildLabelResponse(LABEL_ID, updateLabelRequest.name());

        doReturn(Optional.of(label)).when(labelRepository).findById(label.getId());
        doReturn(false).when(labelRepository)
                .existsByNameAndBoardIdAndIdNot(updateLabelRequest.name(), board.getId(), label.getId());
        doReturn(label).when(labelRepository).save(label);
        doReturn(labelResponse).when(labelResponseMapper).toDto(label);

        LabelResponse actual = labelService.updateLabel(updateLabelRequest, label.getId());

        assertEquals(labelResponse, actual);
        assertEquals(updateLabelRequest.name(), label.getName());
        assertEquals(updateLabelRequest.colour(), label.getColour());

        verify(labelRepository).findById(label.getId());
        verify(labelRepository).existsByNameAndBoardIdAndIdNot(updateLabelRequest.name(), board.getId(), label.getId());
        verify(labelRepository).save(label);
        verify(activityService).publish(board, LABEL_UPDATED, "Updated label %s".formatted(LABEL_NAME));
        verify(labelResponseMapper).toDto(label);
    }

    @Test
    void deleteLabel_whenLabelNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(labelRepository).findById(LABEL_ID);
        
        assertThrows(ObjectNotFoundException.class, () -> labelService.deleteLabel(LABEL_ID));
        
        verify(labelRepository).findById(LABEL_ID);
        verify(labelRepository, never()).delete(any(Label.class));
        verifyNoInteractions(activityService);
    }
    
    @Test
    void deleteLabel_whenValidRequestData_shouldDeleteLabel() {
        Label label = buildLabel(LABEL_ID, LABEL_NAME);
        Board board = label.getBoard();

        doReturn(Optional.of(label)).when(labelRepository).findById(label.getId());

        labelService.deleteLabel(label.getId());

        verify(labelRepository).findById(label.getId());
        verify(labelRepository).delete(label);
        verify(activityService).publish(board, LABEL_DELETED, "Deleted label Bug");
    }
    
    private Label buildLabel(Long id, String name) {
        Label label = new Label();
        label.setId(id);
        label.setName(name);
        label.setBoard(getBoard());

        return label;
    }

    private LabelResponse buildLabelResponse(Long id, String name) {
        return new LabelResponse(id, name, LABEL_COLOUR, BOARD_ID);
    }

    private Board getBoard() {
        Board board = new Board();
        board.setId(BOARD_ID);

        return board;
    }
}