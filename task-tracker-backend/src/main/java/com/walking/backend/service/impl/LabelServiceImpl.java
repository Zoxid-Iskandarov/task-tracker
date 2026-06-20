package com.walking.backend.service.impl;

import com.walking.backend.audit.annotation.TrackActivity;
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
import com.walking.backend.service.BoardService;
import com.walking.backend.service.LabelService;
import com.walking.backend.service.mapper.label.CreateLabelRequestMapper;
import com.walking.backend.service.mapper.label.LabelResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.walking.backend.domain.model.ActivityType.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LabelServiceImpl implements LabelService {
    private final BoardService boardService;
    private final LabelRepository labelRepository;
    private final ActivityService activityService;
    private final CreateLabelRequestMapper createLabelRequestMapper;
    private final LabelResponseMapper labelResponseMapper;
    private final AppProperties appProperties;

    @Override
    @PreAuthorize("@resourceAccessService.canViewBoard(#boardId, principal.id)")
    public List<LabelResponse> getLabels(Long boardId, String name) {
        List<Label> labels = (name != null && !name.isBlank())
                ? labelRepository.findAllByBoardIdAndNameContainingIgnoreCase(boardId, name)
                : labelRepository.findAllByBoardId(boardId);

        return labels.stream()
                .map(labelResponseMapper::toDto)
                .toList();
    }

    @Override
    public Label getLabelById(Long labelId) {
        return labelRepository.findById(labelId)
                .orElseThrow(() -> new ObjectNotFoundException("Label with id %d not found".formatted(labelId)));
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageBoard(#createLabelRequest.boardId(), principal.id)")
    @TrackActivity(type = LABEL_CREATED, description = "'Created label ' + #result.name")
    public LabelResponse createLabel(CreateLabelRequest createLabelRequest) {
        int maxLabelsPerBoard = appProperties.getLabel().getMaxPerBoard();

        if (labelRepository.countByBoardId(createLabelRequest.boardId()) >= maxLabelsPerBoard) {
            throw new LabelLimitExceededException("Board cannot contain more than %d labels"
                    .formatted(maxLabelsPerBoard));
        }

        if (labelRepository.existsByNameAndBoardId(createLabelRequest.name(), createLabelRequest.boardId())) {
            throw new DuplicateException("Label %s already exists in this board".formatted(createLabelRequest.name()));
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
    @PreAuthorize("@resourceAccessService.canManageLabel(#labelId, principal.id)")
    public LabelResponse updateLabel(UpdateLabelRequest labelRequest, Long labelId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ObjectNotFoundException("Label with id %d not found".formatted(labelId)));

        if (labelRepository.existsByNameAndBoardIdAndIdNot(labelRequest.name(), label.getBoard().getId(), labelId)) {
            throw new DuplicateException("Label %s already exists in this board".formatted(labelRequest.name()));
        }

        String oldName = label.getName();
        String newName = labelRequest.name();
        Board board = label.getBoard();

        label.setName(newName);
        label.setColour(labelRequest.colour());

        Label savedLabel = labelRepository.save(label);

        String description = oldName.equals(newName)
                ? "Updated label %s".formatted(newName)
                : "Renamed label from %s to %s".formatted(oldName, newName);

        activityService.publish(board, LABEL_UPDATED, description);

        return labelResponseMapper.toDto(savedLabel);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageLabel(#labelId, principal.id)")
    public void deleteLabel(Long labelId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ObjectNotFoundException("Label with id %d not found".formatted(labelId)));

        Board board = label.getBoard();

        labelRepository.delete(label);

        activityService.publish(board, LABEL_DELETED, "Deleted label %s".formatted(label.getName()));
    }
}
