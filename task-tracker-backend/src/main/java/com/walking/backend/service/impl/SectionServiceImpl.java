package com.walking.backend.service.impl;

import com.walking.backend.audit.annotation.TrackActivity;
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
import com.walking.backend.service.BoardService;
import com.walking.backend.service.SectionService;
import com.walking.backend.service.mapper.section.CreateSectionRequestMapper;
import com.walking.backend.service.mapper.section.SectionResponseMapper;
import com.walking.backend.storage.service.ResourceCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.walking.backend.domain.model.ActivityType.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SectionServiceImpl implements SectionService {
    private final SectionRepository sectionRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final BoardService boardService;
    private final ActivityService activityService;
    private final ResourceCleanupService resourceCleanupService;
    private final CreateSectionRequestMapper createSectionRequestMapper;
    private final SectionResponseMapper sectionResponseMapper;

    @Override
    @PreAuthorize("@resourceAccessService.canViewBoard(#boardId, principal.id)")
    public Page<SectionResponse> getSections(Long boardId, Pageable pageable) {
        return sectionRepository.findAllByBoardId(boardId, pageable)
                .map(sectionResponseMapper::toDto);
    }

    @Override
    public Section getProxySectionById(Long sectionId) {
        return sectionRepository.getReferenceById(sectionId);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canEditBoard(#createSectionRequest.boardId(), principal.id)")
    @TrackActivity(type = SECTION_CREATED, description = "'Created section ' + #result.name")
    public SectionResponse createSection(CreateSectionRequest createSectionRequest) {
        if (sectionRepository.existsSectionByNameAndBoardId(createSectionRequest.name(), createSectionRequest.boardId())) {
            throw new DuplicateException("Section %s already exists in this board"
                    .formatted(createSectionRequest.name()));
        }

        return Optional.of(createSectionRequest)
                .map(createSectionRequestMapper::toEntity)
                .map(section -> {
                    section.setBoard(boardService.getProxyBoardById(createSectionRequest.boardId()));
                    return sectionRepository.save(section);
                })
                .map(sectionResponseMapper::toDto)
                .orElseThrow();
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canEditSection(#sectionId, principal.id)")
    public SectionResponse updateSection(UpdateSectionRequest updateSectionRequest, Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ObjectNotFoundException("Section with id %d not found".formatted(sectionId)));

        if (sectionRepository.existsByNameAndBoardIdAndIdNot(
                updateSectionRequest.name(), section.getBoard().getId(), sectionId)) {
            throw new DuplicateException("Section %s already exists in this board"
                    .formatted(updateSectionRequest.name()));
        }

        String oldName = section.getName();
        String newName = updateSectionRequest.name();
        Board board = section.getBoard();

        section.setName(newName);

        Section savedSection = sectionRepository.save(section);

        activityService.publish(board, SECTION_UPDATED, "Renamed section from %s to %s".formatted(oldName, newName));

        return sectionResponseMapper.toDto(savedSection);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canEditSection(#sectionId, principal.id)")
    public void deleteSection(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ObjectNotFoundException("Section with id %d not found".formatted(sectionId)));

        List<String> filePaths = taskAttachmentRepository.findAllFilePathBySectionId(sectionId);

        Board board = section.getBoard();

        sectionRepository.delete(section);

        activityService.publish(board, SECTION_DELETED, "Deleted section %s".formatted(section.getName()));
        resourceCleanupService.cleanupFiles(filePaths);
    }
}
