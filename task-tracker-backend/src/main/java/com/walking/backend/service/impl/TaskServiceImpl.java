package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.task.*;
import com.walking.backend.domain.exception.*;
import com.walking.backend.domain.model.Label;
import com.walking.backend.domain.model.Task;
import com.walking.backend.repository.TaskRepository;
import com.walking.backend.repository.specification.TaskSpecification;
import com.walking.backend.service.LabelService;
import com.walking.backend.service.SectionService;
import com.walking.backend.service.TaskService;
import com.walking.backend.service.mapper.task.CreateTaskRequestMapper;
import com.walking.backend.service.mapper.task.TaskFullResponseMapper;
import com.walking.backend.service.mapper.task.TaskPreviewResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final SectionService sectionService;
    private final LabelService labelService;
    private final TaskRepository taskRepository;
    private final CreateTaskRequestMapper createTaskRequestMapper;
    private final TaskFullResponseMapper taskFullResponseMapper;
    private final TaskPreviewResponseMapper taskPreviewResponseMapper;

    @Value("${app.label.max-per-task}")
    private final int maxLabelsPerTask;

    @Value("${app.task.position-step}")
    private final double positionStep;

    @Override
    @PreAuthorize("@resourceAccessService.isOwnerOfSection(#sectionId, principal.id)")
    public Page<TaskPreviewResponse> getTasks(Long sectionId, Pageable pageable) {
        Specification<Task> spec = Specification.where(TaskSpecification.hasSectionId(sectionId));

        return taskRepository.findAll(spec, pageable)
                .map(taskPreviewResponseMapper::toDto);
    }

    @Override
    @PreAuthorize("@resourceAccessService.isOwnerOfBoard(#boardId, principal.id)")
    public Page<TaskPreviewResponse> searchTasks(Long boardId, TaskFilter taskFilter, Pageable pageable) {
        Specification<Task> spec = Specification.where(TaskSpecification.hasBoardId(boardId))
                .and(TaskSpecification.hasSectionId(taskFilter.sectionId()))
                .and(TaskSpecification.hasTitle(taskFilter.title()))
                .and(TaskSpecification.hasCompleted(taskFilter.completed()))
                .and(TaskSpecification.hasLabels(taskFilter.labelIds()))
                .and(TaskSpecification.hasCreatedBetween(taskFilter.createdFrom(), taskFilter.createdTo()));

        return taskRepository.findAll(spec, pageable)
                .map(taskPreviewResponseMapper::toDto);
    }

    @Override
    @PreAuthorize("@resourceAccessService.isOwnerOfTask(#taskId, principal.id)")
    public TaskFullResponse getTaskById(Long taskId) {
        return taskRepository.findByIdWithLabels(taskId)
                .map(taskFullResponseMapper::toDto)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id '%d' not found".formatted(taskId)));
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.isOwnerOfSection(#createTaskRequest.sectionId(), principal.id)")
    public TaskFullResponse createTask(CreateTaskRequest createTaskRequest) {
        Task task = createTaskRequestMapper.toEntity(createTaskRequest);
        task.setIsCompleted(false);
        task.setSection(sectionService.getProxySectionById(createTaskRequest.sectionId()));

        Double position = Optional.ofNullable(taskRepository.findMaxPositionBySectionId(createTaskRequest.sectionId()))
                .map(p -> p + positionStep)
                .orElse(positionStep);
        task.setPosition(position);

        Task savedTask = taskRepository.save(task);

        return taskFullResponseMapper.toDto(savedTask);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.isOwnerOfTask(#taskId, principal.id)")
    public TaskFullResponse updateTask(UpdateTaskRequest updateTaskRequest, Long taskId) {
        return taskRepository.findById(taskId)
                .map(task -> {
                    task.setTitle(updateTaskRequest.title());
                    task.setDescription(updateTaskRequest.description());
                    return taskRepository.save(task);
                })
                .map(taskFullResponseMapper::toDto)
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
    public TaskPreviewResponse toggleCompleted(Long taskId) {
        return taskRepository.findById(taskId)
                .map(task -> {
                    task.setIsCompleted(!task.getIsCompleted());
                    return taskRepository.save(task);
                })
                .map(taskPreviewResponseMapper::toDto)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id '%d' not found".formatted(taskId)));
    }

    @Override
    @Transactional
    @PreAuthorize("""
            @resourceAccessService.isOwnerOfTask(#taskId, principal.id) &&
            @resourceAccessService.isOwnerOfSection(#moveTaskRequest.sectionId(), principal.id)
            """)
    public TaskPreviewResponse moveTask(Long taskId, MoveTaskRequest moveTaskRequest) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id '%d' not found".formatted(taskId)));

        Long sectionId = moveTaskRequest.sectionId();
        Task prev = null;
        Task next = null;

        if (moveTaskRequest.prevTaskId() != null) {
            prev = taskRepository.findByIdAndSectionId(moveTaskRequest.prevTaskId(), sectionId)
                    .orElseThrow(() -> new TaskMoveException(
                            "prevTaskId '%d' does not exist in target section".formatted(moveTaskRequest.prevTaskId())
                    ));
        }
        if (moveTaskRequest.nextTaskId() != null) {
            next = taskRepository.findByIdAndSectionId(moveTaskRequest.nextTaskId(), sectionId)
                    .orElseThrow(() -> new TaskMoveException(
                            "nextTaskId '%d' does not exist in target section".formatted(moveTaskRequest.nextTaskId())
                    ));
        }

        if (prev != null && next != null && prev.getId().equals(next.getId())) {
            throw new TaskMoveException("Arguments prevTaskId and nextTaskId cannot be the same");
        }
        if ((prev != null && task.getId().equals(prev.getId())) ||
                (next != null && task.getId().equals(next.getId()))) {
            throw new TaskMoveException("Cannot move relative to itself");
        }

        double newPosition;
        if (prev == null && next == null) {
            newPosition = positionStep;
        } else if (prev == null) {
            newPosition = next.getPosition() - positionStep;
        } else if (next == null) {
            newPosition = prev.getPosition() + positionStep;
        } else {
            if (Math.abs(prev.getPosition() - next.getPosition()) < 0.00001) {
                reindexSection(moveTaskRequest.sectionId());

                prev = taskRepository.findById(moveTaskRequest.prevTaskId()).orElseThrow();
                next = taskRepository.findById(moveTaskRequest.nextTaskId()).orElseThrow();
            }

            newPosition = (next.getPosition() + prev.getPosition()) / 2;
        }

        task.setPosition(newPosition);
        task.setSection(sectionService.getProxySectionById(moveTaskRequest.sectionId()));

        Task movedTask = taskRepository.save(task);

        return taskPreviewResponseMapper.toDto(movedTask);
    }

    @Override
    @Transactional
    @PreAuthorize("""
            @resourceAccessService.isOwnerOfTask(#taskId, principal.id) &&
            @resourceAccessService.isOwnerOfLabel(#labelId, principal.id)
            """)
    public TaskPreviewResponse addLabelToTask(Long taskId, Long labelId) {
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

        return taskPreviewResponseMapper.toDto(task);
    }

    @Override
    @Transactional
    @PreAuthorize("""
            @resourceAccessService.isOwnerOfTask(#taskId, principal.id) &&
            @resourceAccessService.isOwnerOfLabel(#labelId, principal.id)
            """)
    public TaskPreviewResponse deleteLabelFromTask(Long taskId, Long labelId) {
        Task task = taskRepository.findByIdWithLabels(taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id '%d' not found".formatted(taskId)));

        Label label = labelService.getLabelById(labelId);

        if (!task.getSection().getBoard().getId().equals(label.getBoard().getId())) {
            throw new CrossBoardOperationException("Label and Task must belong to the same board");
        }

        task.getLabels().remove(label);
        return taskPreviewResponseMapper.toDto(task);
    }

    private void reindexSection(Long sectionId) {
        List<Task> tasks = taskRepository.findAllBySectionIdOrderByPositionAsc(sectionId);

        double position = positionStep;

        for (Task task : tasks) {
            task.setPosition(position);
            position += positionStep;
        }

        taskRepository.saveAll(tasks);
    }
}
