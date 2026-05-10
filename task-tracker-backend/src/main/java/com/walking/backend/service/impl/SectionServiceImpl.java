package com.walking.backend.service.impl;

import com.walking.backend.audit.annotation.TrackActivity;
import com.walking.backend.domain.dto.section.CreateSectionRequest;
import com.walking.backend.domain.dto.section.SectionResponse;
import com.walking.backend.domain.dto.section.UpdateSectionRequest;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.ActivityType;
import com.walking.backend.domain.model.Board;
import com.walking.backend.domain.model.Section;
import com.walking.backend.domain.model.UserActivity;
import com.walking.backend.repository.SectionRepository;
import com.walking.backend.security.CustomUserDetails;
import com.walking.backend.service.BoardService;
import com.walking.backend.service.SectionService;
import com.walking.backend.service.mapper.section.CreateSectionRequestMapper;
import com.walking.backend.service.mapper.section.SectionResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.walking.backend.domain.model.ActivityType.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SectionServiceImpl implements SectionService {
    private final BoardService boardService;
    private final SectionRepository sectionRepository;
    private final CreateSectionRequestMapper createSectionRequestMapper;
    private final SectionResponseMapper sectionResponseMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

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
            throw new DuplicateException("Section with name '%s' in board with id '%d' already exists"
                    .formatted(createSectionRequest.name(), createSectionRequest.boardId()));
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
                .orElseThrow(() -> new ObjectNotFoundException("Section with id '%d' not found".formatted(sectionId)));

        if (sectionRepository.existsByNameAndBoardIdAndIdNot(
                updateSectionRequest.name(), section.getBoard().getId(), sectionId)) {
            throw new DuplicateException("Section with name '%s' in board with id '%d' already exists"
                    .formatted(updateSectionRequest.name(), section.getBoard().getId()));
        }

        String oldName = section.getName();
        String newName = updateSectionRequest.name();
        Board board = section.getBoard();

        section.setName(newName);

        Section savedSection = sectionRepository.save(section);

        publishActivity(board.getId(), board.getName(),
                SECTION_UPDATED, "Updated section from '%s' to '%s'".formatted(oldName, newName));

        return sectionResponseMapper.toDto(savedSection);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canEditSection(#sectionId, principal.id)")
    public void deleteSection(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ObjectNotFoundException("Section with id '%d' not found".formatted(sectionId)));

        Board board = section.getBoard();

        publishActivity(board.getId(), board.getName(),
                SECTION_DELETED, "Deleted section '%s'".formatted(section.getName()));

        sectionRepository.delete(section);
    }

    private void publishActivity(Long boardId, String boardName, ActivityType type, String description) {
        CustomUserDetails userDetails = getCurrentUser();

        applicationEventPublisher.publishEvent(UserActivity.builder()
                .userId(userDetails.id())
                .username(userDetails.username())
                .boardId(boardId)
                .boardName(boardName)
                .activityType(type)
                .description(description)
                .build());
    }

    private CustomUserDetails getCurrentUser() {
        return (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
