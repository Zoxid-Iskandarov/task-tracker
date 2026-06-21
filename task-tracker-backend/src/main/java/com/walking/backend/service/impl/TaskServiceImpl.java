package com.walking.backend.service.impl;

import com.walking.backend.audit.annotation.TrackActivity;
import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.task.*;
import com.walking.backend.domain.dto.user.UserShortResponse;
import com.walking.backend.domain.exception.*;
import com.walking.backend.domain.model.*;
import com.walking.backend.props.AppProperties;
import com.walking.backend.repository.TaskRepository;
import com.walking.backend.repository.specification.TaskSpecification;
import com.walking.backend.service.LabelService;
import com.walking.backend.service.SectionService;
import com.walking.backend.service.TaskService;
import com.walking.backend.service.UserService;
import com.walking.backend.service.mapper.task.CreateTaskRequestMapper;
import com.walking.backend.service.mapper.task.TaskFullResponseMapper;
import com.walking.backend.service.mapper.task.TaskPreviewResponseMapper;
import com.walking.backend.storage.service.ResourceCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.walking.backend.domain.model.ActivityType.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final SectionService sectionService;
    private final UserService userService;
    private final LabelService labelService;
    private final ActivityService activityService;
    private final ResourceCleanupService resourceCleanupService;
    private final CreateTaskRequestMapper createTaskRequestMapper;
    private final TaskFullResponseMapper taskFullResponseMapper;
    private final TaskPreviewResponseMapper taskPreviewResponseMapper;
    private final AppProperties appProperties;

    @Override
    @PreAuthorize("@resourceAccessService.canViewSection(#sectionId, principal.id)")
    public Page<TaskPreviewResponse> getTasks(Long sectionId, Pageable pageable) {
        Specification<Task> spec = Specification.where(TaskSpecification.hasSectionId(sectionId));

        Page<Task> tasks = taskRepository.findAll(spec, pageable);

        Map<Long, List<UserShortResponse>> assigneesByTaskId = loadAssigneesBatch(tasks.getContent());

        return tasks.map(task ->
                taskPreviewResponseMapper.toDto(task, assigneesByTaskId.getOrDefault(task.getId(), List.of())));
    }

    @Override
    @PreAuthorize("@resourceAccessService.canViewBoard(#boardId, principal.id)")
    public Page<TaskPreviewResponse> searchTasks(Long boardId, TaskFilter taskFilter, Pageable pageable) {
        Specification<Task> spec = Specification.where(TaskSpecification.hasBoardId(boardId))
                .and(TaskSpecification.hasSectionId(taskFilter.sectionId()))
                .and(TaskSpecification.hasTitle(taskFilter.title()))
                .and(TaskSpecification.hasCompleted(taskFilter.completed()))
                .and(TaskSpecification.hasLabels(taskFilter.labelIds()))
                .and(TaskSpecification.hasAssignees(taskFilter.assigneeIds()))
                .and(TaskSpecification.hasDueDate(taskFilter.dueDateFrom(), taskFilter.dueDateTo()))
                .and(TaskSpecification.hasCreatedBetween(taskFilter.createdFrom(), taskFilter.createdTo()));

        Page<Task> tasks = taskRepository.findAll(spec, pageable);

        Map<Long, List<UserShortResponse>> assigneesByTaskId = loadAssigneesBatch(tasks.getContent());

        return tasks.map(task ->
                taskPreviewResponseMapper.toDto(task, assigneesByTaskId.getOrDefault(task.getId(), List.of())));
    }

    @Override
    @PreAuthorize("@resourceAccessService.canViewTask(#taskId, principal.id)")
    public TaskFullResponse getTaskById(Long taskId) {
        return taskRepository.findByIdWithLabels(taskId)
                .map(task -> taskFullResponseMapper.toDto(task, loadAssignees(task)))
                .orElseThrow(() -> new ObjectNotFoundException("Task with id %d not found".formatted(taskId)));
    }

    @Override
    public Task getProxyTaskById(Long taskId) {
        return taskRepository.getReferenceById(taskId);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canEditSection(#createTaskRequest.sectionId(), principal.id)")
    @TrackActivity(type = TASK_CREATED, description = "'Created task ' + #result.title")
    public TaskFullResponse createTask(CreateTaskRequest createTaskRequest) {
        Task task = createTaskRequestMapper.toEntity(createTaskRequest);
        task.setIsCompleted(false);
        task.setSection(sectionService.getProxySectionById(createTaskRequest.sectionId()));

        assignUsersToTask(createTaskRequest.sectionId(), createTaskRequest.assigneeIds(), task);

        double positionStep = appProperties.getTask().getPositionStep();

        Double position = Optional.ofNullable(taskRepository.findMaxPositionBySectionId(createTaskRequest.sectionId()))
                .map(p -> p + positionStep)
                .orElse(positionStep);
        task.setPosition(position);

        Task savedTask = taskRepository.save(task);

        return taskFullResponseMapper.toDto(savedTask, loadAssignees(task));
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canEditTask(#taskId, principal.id)")
    public TaskFullResponse updateTask(UpdateTaskRequest updateTaskRequest, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id %d not found".formatted(taskId)));

        String oldTitle = task.getTitle();
        String newTitle = updateTaskRequest.title();
        Board board = task.getSection().getBoard();

        task.setTitle(newTitle);
        task.setDescription(updateTaskRequest.description());
        task.setDueDate(updateTaskRequest.dueDate());

        assignUsersToTask(task.getSection().getId(), updateTaskRequest.assigneeIds(), task);

        Task updatedTask = taskRepository.save(task);

        String description = oldTitle.equals(newTitle)
                ? "Updated task %s".formatted(newTitle)
                : "Renamed task from %s to %s".formatted(oldTitle, newTitle);

        activityService.publish(board, TASK_UPDATED, description);

        return taskFullResponseMapper.toDto(updatedTask, loadAssignees(task));
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canEditTask(#taskId, principal.id)")
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findByIdWithAttachments(taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id %d not found".formatted(taskId)));

        Board board = task.getSection().getBoard();

        List<String> filePaths = task.getAttachments()
                .stream()
                .map(TaskAttachment::getFilePath)
                .toList();

        taskRepository.delete(task);

        activityService.publish(board, TASK_DELETED, "Deleted task %s".formatted(task.getTitle()));
        resourceCleanupService.cleanupFiles(filePaths);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canToggleTask(#taskId, principal.id)")
    public TaskPreviewResponse toggleCompleted(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id %d not found".formatted(taskId)));

        task.setIsCompleted(!task.getIsCompleted());
        Task toggledTask = taskRepository.save(task);

        Board board = toggledTask.getSection().getBoard();

        ActivityType type = toggledTask.getIsCompleted()
                ? TASK_COMPLETED
                : TASK_REOPENED;

        String description = toggledTask.getIsCompleted()
                ? "Completed task %s".formatted(task.getTitle())
                : "Reopened task %s".formatted(task.getTitle());

        activityService.publish(board, type, description);

        return taskPreviewResponseMapper.toDto(toggledTask, loadAssignees(task));
    }

    @Override
    @Transactional
    @PreAuthorize("""
            @resourceAccessService.canEditTask(#taskId, principal.id) &&
            @resourceAccessService.canEditSection(#moveTaskRequest.sectionId(), principal.id)
            """)
    public TaskPreviewResponse moveTask(Long taskId, MoveTaskRequest moveTaskRequest) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id %d not found".formatted(taskId)));

        Long sectionId = moveTaskRequest.sectionId();
        Task prev = null;
        Task next = null;

        if (moveTaskRequest.prevTaskId() != null) {
            prev = taskRepository.findByIdAndSectionId(moveTaskRequest.prevTaskId(), sectionId)
                    .orElseThrow(() -> new TaskMoveException(
                            "Previous task %d does not exist in target section".formatted(moveTaskRequest.prevTaskId())
                    ));
        }
        if (moveTaskRequest.nextTaskId() != null) {
            next = taskRepository.findByIdAndSectionId(moveTaskRequest.nextTaskId(), sectionId)
                    .orElseThrow(() -> new TaskMoveException(
                            "Next task %d does not exist in target section".formatted(moveTaskRequest.nextTaskId())
                    ));
        }

        if (prev != null && next != null && prev.getId().equals(next.getId())) {
            throw new TaskMoveException("Previous and next tasks cannot be the same");
        }
        if ((prev != null && task.getId().equals(prev.getId())) ||
                (next != null && task.getId().equals(next.getId()))) {
            throw new TaskMoveException("Cannot move relative to itself");
        }

        double positionStep = appProperties.getTask().getPositionStep();
        double newPosition;

        if (prev == null && next == null) {
            newPosition = positionStep;
        } else if (prev == null) {
            newPosition = next.getPosition() - positionStep;
        } else if (next == null) {
            newPosition = prev.getPosition() + positionStep;
        } else {
            if (Math.abs(prev.getPosition() - next.getPosition()) < 0.00001) {
                reindexSection(sectionId);

                prev = taskRepository.findById(moveTaskRequest.prevTaskId()).orElseThrow();
                next = taskRepository.findById(moveTaskRequest.nextTaskId()).orElseThrow();
            }

            newPosition = (next.getPosition() + prev.getPosition()) / 2;
        }

        Section oldSection = task.getSection();
        Board board = oldSection.getBoard();

        task.setPosition(newPosition);
        task.setSection(sectionService.getProxySectionById(sectionId));

        Task movedTask = taskRepository.save(task);

        if (!oldSection.getId().equals(sectionId)) {
            activityService.publish(board, TASK_MOVED,
                    "Moved task from section %s to %s".formatted(oldSection.getName(), movedTask.getSection().getName()));
        }

        return taskPreviewResponseMapper.toDto(movedTask, loadAssignees(task));
    }

    @Override
    @Transactional
    @PreAuthorize("""
            @resourceAccessService.canEditTask(#taskId, principal.id) &&
            @resourceAccessService.canUseLabel(#labelId, principal.id)
            """)
    public TaskPreviewResponse addLabelToTask(Long taskId, Long labelId) {
        Task task = taskRepository.findByIdWithLabels(taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id %d not found".formatted(taskId)));

        int maxLabelsPerTask = appProperties.getLabel().getMaxPerTask();

        if (task.getLabels().size() >= maxLabelsPerTask) {
            throw new LabelLimitExceededException("Task cannot contain more than %d labels".formatted(maxLabelsPerTask));
        }

        Label label = labelService.getLabelById(labelId);

        if (!task.getSection().getBoard().getId().equals(label.getBoard().getId())) {
            throw new CrossBoardOperationException("Task and label must belong to the same board");
        }

        if (!task.getLabels().add(label)) {
            throw new DuplicateException("Label with id %d is already added to task %d".formatted(labelId, taskId));
        }

        Board board = label.getBoard();

        activityService.publish(board, TASK_LABEL_ADDED,
                "Added label %s to task %s".formatted(label.getName(), task.getTitle()));

        return taskPreviewResponseMapper.toDto(task, loadAssignees(task));
    }

    @Override
    @Transactional
    @PreAuthorize("""
            @resourceAccessService.canEditTask(#taskId, principal.id) &&
            @resourceAccessService.canUseLabel(#labelId, principal.id)
            """)
    public TaskPreviewResponse deleteLabelFromTask(Long taskId, Long labelId) {
        Task task = taskRepository.findByIdWithLabels(taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Task with id %d not found".formatted(taskId)));

        Label label = labelService.getLabelById(labelId);

        if (!task.getSection().getBoard().getId().equals(label.getBoard().getId())) {
            throw new CrossBoardOperationException("Task and label must belong to the same board");
        }

        task.getLabels().remove(label);

        Board board = label.getBoard();

        activityService.publish(board, TASK_LABEL_DELETED,
                "Removed label %s from task %s".formatted(label.getName(), task.getTitle()));

        return taskPreviewResponseMapper.toDto(task, loadAssignees(task));
    }

    private void reindexSection(Long sectionId) {
        List<Task> tasks = taskRepository.findAllBySectionIdOrderByPositionAsc(sectionId);

        double positionStep = appProperties.getTask().getPositionStep();
        double position = positionStep;

        for (Task task : tasks) {
            task.setPosition(position);
            position += positionStep;
        }

        taskRepository.saveAll(tasks);
    }

    private void assignUsersToTask(Long sectionId, Set<Long> assigneeIds, Task task) {
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            task.getAssignees().clear();
            return;
        }

        Set<User> assignees = userService.getBoardMembersForTask(sectionId, assigneeIds);

        Set<Long> foundIds = assignees.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        List<Long> invalidIds = assigneeIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new InvalidTaskAssigneeException("Users with id %s are not members of the board".formatted(invalidIds));
        }

        task.setAssignees(assignees);
    }

    private List<UserShortResponse> loadAssignees(Task task) {
        if (task.getAssignees().isEmpty()) return List.of();

        Set<Long> userIds = task.getAssignees()
                .stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        return userService.getUserShortsByIds(userIds);
    }

    private Map<Long, List<UserShortResponse>> loadAssigneesBatch(List<Task> tasks) {
        Set<Long> taskIds = tasks.stream()
                .map(Task::getId)
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) return Map.of();

        return userService.getAssigneeByTaskIds(taskIds);
    }
}
