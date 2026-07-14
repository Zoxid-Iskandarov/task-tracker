package com.walking.backend.service;

import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.section.CreateSectionRequest;
import com.walking.backend.domain.dto.section.SectionResponse;
import com.walking.backend.domain.dto.section.UpdateSectionRequest;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Board;
import com.walking.backend.domain.model.Section;
import com.walking.backend.repository.SectionRepository;
import com.walking.backend.repository.TaskAttachmentRepository;
import com.walking.backend.service.impl.SectionServiceImpl;
import com.walking.backend.service.mapper.section.CreateSectionRequestMapper;
import com.walking.backend.service.mapper.section.SectionResponseMapper;
import com.walking.backend.storage.service.ResourceCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.walking.backend.domain.model.ActivityType.SECTION_DELETED;
import static com.walking.backend.domain.model.ActivityType.SECTION_UPDATED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {
    private static final Long BOARD_ID = 2L;
    private static final Long SECTION_ID = 1L;
    private static final String NAME = "In Process";

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private TaskAttachmentRepository taskAttachmentRepository;

    @Mock
    private BoardService boardService;

    @Mock
    private ActivityService activityService;

    @Mock
    private ResourceCleanupService resourceCleanupService;

    @Mock
    private CreateSectionRequestMapper createSectionRequestMapper;

    @Mock
    private SectionResponseMapper sectionResponseMapper;

    @InjectMocks
    private SectionServiceImpl sectionService;

    @Test
    void getSections_whenValidRequestData_shouldReturnPageOfSectionResponse() {
        var section1 = buildSection(1L, "In Process");
        var section2 = buildSection(2L, "To Do");

        var pageable = PageRequest.of(0, 10);
        var sections = new PageImpl<>(List.of(section1, section2), pageable, 2L);

        var sectionResponse1 = buildSectionResponse(1L, "In Process");
        var sectionResponse2 = buildSectionResponse(2L, "To Do");

        doReturn(sections).when(sectionRepository).findAllByBoardId(BOARD_ID, pageable);
        doReturn(sectionResponse1).when(sectionResponseMapper).toDto(section1);
        doReturn(sectionResponse2).when(sectionResponseMapper).toDto(section2);

        Page<SectionResponse> actual = sectionService.getSections(BOARD_ID, pageable);

        assertTrue(actual.hasContent());
        assertEquals(2, actual.getContent().size());
        assertEquals(sectionResponse1, actual.getContent().get(0));
        assertEquals(sectionResponse2, actual.getContent().get(1));

        verify(sectionRepository).findAllByBoardId(BOARD_ID, pageable);
        verify(sectionResponseMapper).toDto(section1);
        verify(sectionResponseMapper).toDto(section2);
    }

    @Test
    void createSection_whenSectionByNameAlreadyExistsInBoard_shouldThrowDuplicateException() {
        var request = new CreateSectionRequest(NAME, BOARD_ID);

        doReturn(true).when(sectionRepository).existsSectionByNameAndBoardId(request.name(), request.boardId());

        assertThrows(DuplicateException.class, () -> sectionService.createSection(request));

        verify(sectionRepository).existsSectionByNameAndBoardId(request.name(), request.boardId());
        verify(sectionRepository, never()).save(any(Section.class));
        verifyNoInteractions(createSectionRequestMapper, boardService, sectionResponseMapper);
    }

    @Test
    void createSection_whenValidRequestData_shouldCreateSectionAndReturnSectionResponse() {
        var request = new CreateSectionRequest(NAME, BOARD_ID);
        var section = buildSection(SECTION_ID, NAME);
        var board = getBoard();
        var sectionResponse = buildSectionResponse(SECTION_ID, NAME);

        doReturn(false).when(sectionRepository).existsSectionByNameAndBoardId(request.name(), request.boardId());
        doReturn(section).when(createSectionRequestMapper).toEntity(request);
        doReturn(board).when(boardService).getProxyBoardById(BOARD_ID);
        doReturn(section).when(sectionRepository).save(section);
        doReturn(sectionResponse).when(sectionResponseMapper).toDto(section);

        SectionResponse actual = sectionService.createSection(request);

        assertEquals(sectionResponse, actual);
        assertEquals(board, section.getBoard());

        verify(sectionRepository).existsSectionByNameAndBoardId(request.name(), request.boardId());
        verify(createSectionRequestMapper).toEntity(request);
        verify(boardService).getProxyBoardById(BOARD_ID);
        verify(sectionRepository).save(section);
        verify(sectionResponseMapper).toDto(section);
    }

    @Test
    void updateSection_whenSectionNotFound_shouldThrowObjectNotFoundException() {
        var updateSectionRequest = new UpdateSectionRequest(NAME);

        doReturn(Optional.empty()).when(sectionRepository).findById(SECTION_ID);

        assertThrows(ObjectNotFoundException.class, () -> sectionService.updateSection(updateSectionRequest, SECTION_ID));

        verify(sectionRepository).findById(SECTION_ID);
        verify(sectionRepository, never()).existsByNameAndBoardIdAndIdNot(updateSectionRequest.name(), BOARD_ID, SECTION_ID);
        verify(sectionRepository, never()).save(any(Section.class));
        verifyNoInteractions(activityService, sectionResponseMapper);
    }

    @Test
    void updateSection_whenSectionByNameAlreadyExistsInBoard_shouldThrowDuplicateException() {
        var updateSectionRequest = new UpdateSectionRequest(NAME);
        var section = buildSection(SECTION_ID, NAME);

        doReturn(Optional.of(section)).when(sectionRepository).findById(SECTION_ID);
        doReturn(true).when(sectionRepository).existsByNameAndBoardIdAndIdNot(updateSectionRequest.name(), BOARD_ID, SECTION_ID);

        assertThrows(DuplicateException.class, () -> sectionService.updateSection(updateSectionRequest, SECTION_ID));

        verify(sectionRepository).findById(SECTION_ID);
        verify(sectionRepository).existsByNameAndBoardIdAndIdNot(updateSectionRequest.name(), BOARD_ID, SECTION_ID);
        verify(sectionRepository, never()).save(any(Section.class));
        verifyNoInteractions(activityService, sectionResponseMapper);
    }

    @Test
    void updateSection_whenValidRequestData_shouldUpdateSectionAndReturnSectionResponse() {
        var request = new UpdateSectionRequest(NAME);
        var section = buildSection(SECTION_ID, "Doing");
        var sectionResponse = buildSectionResponse(SECTION_ID, NAME);

        doReturn(Optional.of(section)).when(sectionRepository).findById(SECTION_ID);
        doReturn(false).when(sectionRepository).existsByNameAndBoardIdAndIdNot(request.name(), BOARD_ID, SECTION_ID);
        doReturn(section).when(sectionRepository).save(section);
        doReturn(sectionResponse).when(sectionResponseMapper).toDto(section);

        SectionResponse actual = sectionService.updateSection(request, SECTION_ID);

        assertEquals(sectionResponse, actual);

        verify(sectionRepository).findById(SECTION_ID);
        verify(sectionRepository).existsByNameAndBoardIdAndIdNot(request.name(), BOARD_ID, SECTION_ID);
        verify(sectionRepository).save(section);
        verify(activityService).publish(
                any(Board.class), eq(SECTION_UPDATED), eq("Renamed section from Doing to In Process"));
        verify(sectionResponseMapper).toDto(section);
    }

    @Test
    void deleteSection_whenSectionNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(sectionRepository).findById(SECTION_ID);

        assertThrows(ObjectNotFoundException.class, () -> sectionService.deleteSection(SECTION_ID));

        verify(sectionRepository).findById(SECTION_ID);
        verify(sectionRepository, never()).delete(any(Section.class));
        verifyNoInteractions(taskAttachmentRepository, activityService, resourceCleanupService);
    }

    @Test
    void deleteSection_whenValidRequestData_shouldDeleteSection() {
        var section = buildSection(SECTION_ID, NAME);
        var filePaths = List.of("path/to/file1.png", "path/to/file2.png");

        doReturn(Optional.of(section)).when(sectionRepository).findById(SECTION_ID);
        doReturn(filePaths).when(taskAttachmentRepository).findAllFilePathBySectionId(section.getId());

        sectionService.deleteSection(SECTION_ID);

        verify(sectionRepository).findById(SECTION_ID);
        verify(taskAttachmentRepository).findAllFilePathBySectionId(section.getId());
        verify(sectionRepository).delete(section);
        verify(activityService).publish(any(Board.class), eq(SECTION_DELETED), eq("Deleted section In Process"));
        verify(resourceCleanupService).cleanupFiles(filePaths);
    }

    private SectionResponse buildSectionResponse(Long sectionId, String name) {
        return new SectionResponse(sectionId, name, BOARD_ID, LocalDateTime.now(), LocalDateTime.now());
    }

    private Section buildSection(Long sectionId, String name) {
        Section section = new Section();
        section.setId(sectionId);
        section.setName(name);
        section.setBoard(getBoard());

        return section;
    }

    private Board getBoard() {
        Board board = new Board();
        board.setId(BOARD_ID);

        return board;
    }
}