package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.label.CreateLabelRequest;
import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.dto.label.UpdateLabelRequest;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.LabelLimitExceededException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Label;
import com.walking.backend.repository.LabelRepository;
import com.walking.backend.service.BoardService;
import com.walking.backend.service.LabelService;
import com.walking.backend.service.mapper.label.CreateLabelRequestMapper;
import com.walking.backend.service.mapper.label.LabelResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LabelServiceImpl implements LabelService {
    private final BoardService boardService;
    private final LabelRepository labelRepository;
    private final CreateLabelRequestMapper createLabelRequestMapper;
    private final LabelResponseMapper labelResponseMapper;

    @Value("${app.label.max-per-board}")
    private final int maxLabelsPerBoard;

    @Override
    @PreAuthorize("@resourceAccessService.isOwnerOfBoard(#boardId, principal.id)")
    public List<LabelResponse> getLabels(Long boardId) {
        return labelRepository.findAllByBoardId(boardId)
                .stream()
                .map(labelResponseMapper::toDto)
                .toList();
    }

    @Override
    public Label getLabelById(Long labelId) {
        return labelRepository.findById(labelId)
                .orElseThrow(() -> new ObjectNotFoundException("Label with id '%d' not found".formatted(labelId)));
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.isOwnerOfBoard(#createLabelRequest.boardId(), principal.id)")
    public LabelResponse createLabel(CreateLabelRequest createLabelRequest) {
        if (labelRepository.countByBoardId(createLabelRequest.boardId()) >= maxLabelsPerBoard) {
            throw new LabelLimitExceededException("Cannot add more than 100 labels to a board");
        }

        if (labelRepository.existsByNameAndBoardId(createLabelRequest.name(), createLabelRequest.boardId())) {
            throw new DuplicateException("Label with name '%s' in board with id '%d' already exists"
                    .formatted(createLabelRequest.name(), createLabelRequest.boardId()));
        }

        return Optional.of(createLabelRequest)
                .map(createLabelRequestMapper::toEntity)
                .map(label -> {
                    label.setBoard(boardService.getProxyBoardById(createLabelRequest.boardId()));
                    return labelRepository.save(label);
                })
                .map(labelResponseMapper::toDto)
                .orElseThrow();
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.isOwnerOfLabel(#labelId, principal.id)")
    public LabelResponse updateLabel(UpdateLabelRequest labelRequest, Long labelId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ObjectNotFoundException("Label with id '%d' not found".formatted(labelId)));

        if (labelRepository.existsByNameAndBoardIdAndIdNot(labelRequest.name(), label.getBoard().getId(), labelId)) {
            throw new DuplicateException("Label with name '%s' in board with id '%d' already exists"
                    .formatted(labelRequest.name(), label.getBoard().getId()));
        }

        return Optional.of(label)
                .map(labelEntity -> {
                    labelEntity.setName(labelRequest.name());
                    labelEntity.setColour(labelRequest.colour());
                    return labelRepository.save(labelEntity);
                })
                .map(labelResponseMapper::toDto)
                .orElseThrow();
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.isOwnerOfLabel(#labelId, principal.id)")
    public void deleteLabel(Long labelId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ObjectNotFoundException("Label with id '%d' not found"));

        labelRepository.delete(label);
    }
}
