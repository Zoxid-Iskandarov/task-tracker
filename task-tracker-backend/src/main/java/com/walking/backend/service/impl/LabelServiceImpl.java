package com.walking.backend.service.impl;

import com.walking.backend.audit.annotation.TrackActivity;
import com.walking.backend.domain.dto.label.CreateLabelRequest;
import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.dto.label.UpdateLabelRequest;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.LabelLimitExceededException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.ActivityType;
import com.walking.backend.domain.model.Board;
import com.walking.backend.domain.model.Label;
import com.walking.backend.domain.model.UserActivity;
import com.walking.backend.repository.LabelRepository;
import com.walking.backend.security.principal.CustomUserDetails;
import com.walking.backend.service.BoardService;
import com.walking.backend.service.LabelService;
import com.walking.backend.service.mapper.label.CreateLabelRequestMapper;
import com.walking.backend.service.mapper.label.LabelResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final CreateLabelRequestMapper createLabelRequestMapper;
    private final LabelResponseMapper labelResponseMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${app.label.max-per-board}")
    private final int maxLabelsPerBoard;

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
                .orElseThrow(() -> new ObjectNotFoundException("Label with id '%d' not found".formatted(labelId)));
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageBoard(#createLabelRequest.boardId(), principal.id)")
    @TrackActivity(type = LABEL_CREATED, description = "'Created label ' + #result.name")
    public LabelResponse createLabel(CreateLabelRequest createLabelRequest) {
        if (labelRepository.countByBoardId(createLabelRequest.boardId()) >= maxLabelsPerBoard) {
            throw new LabelLimitExceededException("Cannot add more than '%d' labels to a board"
                    .formatted(maxLabelsPerBoard));
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
    @PreAuthorize("@resourceAccessService.canManageLabel(#labelId, principal.id)")
    public LabelResponse updateLabel(UpdateLabelRequest labelRequest, Long labelId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ObjectNotFoundException("Label with id '%d' not found".formatted(labelId)));

        if (labelRepository.existsByNameAndBoardIdAndIdNot(labelRequest.name(), label.getBoard().getId(), labelId)) {
            throw new DuplicateException("Label with name '%s' in board with id '%d' already exists"
                    .formatted(labelRequest.name(), label.getBoard().getId()));
        }

        String oldName = label.getName();
        String newName = labelRequest.name();
        Board board = label.getBoard();

        label.setName(newName);
        label.setColour(labelRequest.colour());

        Label savedLabel = labelRepository.save(label);

        String description = oldName.equals(newName)
                ? "Updated label '%s'".formatted(newName)
                : "Updated label from '%s' to '%s'".formatted(oldName, newName);

        publishActivity(board.getId(), board.getName(), LABEL_UPDATED, description);

        return labelResponseMapper.toDto(savedLabel);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageLabel(#labelId, principal.id)")
    public void deleteLabel(Long labelId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ObjectNotFoundException("Label with id '%d' not found".formatted(labelId)));

        Board board = label.getBoard();

        publishActivity(board.getId(), board.getName(), LABEL_DELETED, "Deleted label '%s'".formatted(label.getName()));

        labelRepository.delete(label);
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
