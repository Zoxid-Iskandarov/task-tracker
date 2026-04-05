package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.section.CreateSectionRequest;
import com.walking.backend.domain.dto.section.SectionResponse;
import com.walking.backend.domain.dto.section.UpdateSectionRequest;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Section;
import com.walking.backend.repository.SectionRepository;
import com.walking.backend.repository.specification.SectionSpecification;
import com.walking.backend.service.BoardService;
import com.walking.backend.service.SectionService;
import com.walking.backend.service.mapper.section.CreateSectionRequestMapper;
import com.walking.backend.service.mapper.section.SectionResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SectionServiceImpl implements SectionService {
    private final BoardService boardService;
    private final SectionRepository sectionRepository;
    private final CreateSectionRequestMapper createSectionRequestMapper;
    private final SectionResponseMapper sectionResponseMapper;

    @Override
    @PreAuthorize("@resourceAccessService.isOwnerOfBoard(#boardId, principal.id)")
    public Page<SectionResponse> getSections(Long boardId, Pageable pageable) {
        Specification<Section> spec = Specification.where(SectionSpecification.hasBoardId(boardId));

        return sectionRepository.findAll(spec, pageable)
                .map(sectionResponseMapper::toDto);
    }

    @Override
    public Section getSectionById(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ObjectNotFoundException("Section with id '%d' not found".formatted(sectionId)));
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.isOwnerOfBoard(#createSectionRequest.boardId(), principal.id)")
    public SectionResponse createSection(CreateSectionRequest createSectionRequest) {
        if (sectionRepository.existsSectionByNameAndBoardId(createSectionRequest.name(), createSectionRequest.boardId())) {
            throw new DuplicateException("Section with name '%s' in board with id '%d' already exists"
                    .formatted(createSectionRequest.name(), createSectionRequest.boardId()));
        }

        return Optional.of(createSectionRequest)
                .map(createSectionRequestMapper::toEntity)
                .map(section -> {
                    section.setBoard(boardService.getBoardById(createSectionRequest.boardId()));
                    return sectionRepository.save(section);
                })
                .map(sectionResponseMapper::toDto)
                .orElseThrow();
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.isOwnerOfSection(#sectionId, principal.id)")
    public SectionResponse updateSection(UpdateSectionRequest updateSectionRequest, Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ObjectNotFoundException("Section with id '%d' not found".formatted(sectionId)));

        if (sectionRepository.existsByNameAndBoardIdAndIdNot(
                updateSectionRequest.name(), section.getBoard().getId(), sectionId)) {
            throw new DuplicateException("Section with name '%s' in board with id '%d' already exists"
                    .formatted(updateSectionRequest.name(), section.getBoard().getId()));
        }

        section.setName(updateSectionRequest.name());
        return sectionResponseMapper.toDto(sectionRepository.save(section));
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.isOwnerOfSection(#sectionId, principal.id)")
    public void deleteSection(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ObjectNotFoundException("Section with id '%d' not found".formatted(sectionId)));

        sectionRepository.delete(section);
    }
}
