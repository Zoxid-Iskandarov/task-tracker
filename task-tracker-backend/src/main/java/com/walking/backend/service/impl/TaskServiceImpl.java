package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.task.TaskRequest;
import com.walking.backend.domain.dto.task.TaskResponse;
import com.walking.backend.domain.exception.CrossBoardOperationException;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.LabelLimitExceededException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Label;
import com.walking.backend.domain.model.Task;
import com.walking.backend.repository.TaskRepository;
import com.walking.backend.repository.specification.TaskSpecification;
import com.walking.backend.service.LabelService;
import com.walking.backend.service.SectionService;
import com.walking.backend.service.TaskService;
import com.walking.backend.service.mapper.task.TaskRequestMapper;
import com.walking.backend.service.mapper.task.TaskResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
public class TaskServiceImpl implements TaskService {
    private final SectionService sectionService;
    private final LabelService labelService;
    private final TaskRepository taskRepository;
    private final TaskRequestMapper taskRequestMapper;
    private final TaskResponseMapper taskResponseMapper;

    @Value("${app.label.max-per-task}")
    private final int maxLabelsPerTask;

    @Override
    @PreAuthorize("@resourceAccessService.isOwnerOfSection(#sectionId, principal.id)")
    public Page<TaskResponse> getTasks(Long sectionId, Pageable pageable) {
        Specification<Task> spec = Specification.where(TaskSpecification.hasSectionId(sectionId));

        return taskRepository.findAll(spec, pageable)
                .map(taskResponseMapper::toDto);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.isOwnerOfSection(#taskRequest.sectionId(), principal.id)")
    public TaskResponse createTask(TaskRequest taskRequest) {
        return Optional.of(taskRequest)
                .map(taskRequestMapper::toEntity)
                .map(task -> {
                    task.setIsCompleted(false);
                    task.setSection(sectionService.getProxySectionById(taskRequest.sectionId()));
                    return taskRepository.save(task);
                })
                .map(taskResponseMapper::toDto)
                .orElseThrow();
    }

    @Override
    @Transactional
    @PreAuthorize("""
            @resourceAccessService.isOwnerOfSection(#taskRequest.sectionId(), principal.id) &&
            @resourceAccessService.isOwnerOfTask(#taskId, principal.id)
            """)
    public TaskResponse updateTask(TaskRequest taskRequest, Long taskId) {
        return taskRepository.findById(taskId)
                .map(task -> {
                    task.setTitle(taskRequest.title());
                    task.setDescription(taskRequest.description());
                    task.setSection(sectionService.getProxySectionById(taskRequest.sectionId()));
                    return taskRepository.save(task);
                })
                .map(taskResponseMapper::toDto)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id '%d' not found".formatted(taskId)));
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.isOwnerOfTask(#taskId, principal.id)")
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id '%d' not found".formatted(taskId)));

        taskRepository.delete(task);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.isOwnerOfTask(#taskId, principal.id)")
    public TaskResponse toggleCompleted(Long taskId) {
        return taskRepository.findById(taskId)
                .map(task -> {
                    task.setIsCompleted(!task.getIsCompleted());
                    return taskRepository.save(task);
                })
                .map(taskResponseMapper::toDto)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id '%d' not found".formatted(taskId)));
    }

    @Override
    @Transactional
    @PreAuthorize("""
            @resourceAccessService.isOwnerOfTask(#taskId, principal.id) && 
            @resourceAccessService.isOwnerOfSection(#sectionId, principal.id)
            """)
    public TaskResponse moveTask(Long taskId, Long sectionId) {
        return taskRepository.findById(taskId)
                .map(task -> {
                    task.setSection(sectionService.getProxySectionById(sectionId));
                    return taskRepository.save(task);
                })
                .map(taskResponseMapper::toDto)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id '%d' not found".formatted(taskId)));
    }

    @Override
    @Transactional
    @PreAuthorize("""
            @resourceAccessService.isOwnerOfTask(#taskId, principal.id) &&
            @resourceAccessService.isOwnerOfLabel(#labelId, principal.id)
            """)
    public TaskResponse addLabelToTask(Long taskId, Long labelId) {
        Task task = taskRepository.findByIdWithLabels(taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id '%d' not found".formatted(taskId)));

        if (task.getLabels().size() >= maxLabelsPerTask) {
            throw new LabelLimitExceededException("Task cannot have more than '%d' labels".formatted(maxLabelsPerTask));
        }

        Label label = labelService.getLabelById(labelId);

        if (!task.getSection().getBoard().getId().equals(label.getBoard().getId())) {
            throw new CrossBoardOperationException("Label and Task must belong to the same board");
        }

        if (!task.getLabels().add(label)) {
            throw new DuplicateException("Label with id '%d' already added to task with id '%d'"
                    .formatted(labelId, taskId));
        }

        return taskResponseMapper.toDto(task);
    }

    @Override
    @Transactional
    @PreAuthorize("""
            @resourceAccessService.isOwnerOfTask(#taskId, principal.id) &&
            @resourceAccessService.isOwnerOfLabel(#labelId, principal.id)
            """)
    public TaskResponse deleteLabelFromTask(Long taskId, Long labelId) {
        Task task = taskRepository.findByIdWithLabels(taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id '%d' not found".formatted(taskId)));

        Label label = labelService.getLabelById(labelId);

        if (!task.getSection().getBoard().getId().equals(label.getBoard().getId())) {
            throw new CrossBoardOperationException("Label and Task must belong to the same board");
        }

        task.getLabels().remove(label);
        return taskResponseMapper.toDto(task);
    }
}
