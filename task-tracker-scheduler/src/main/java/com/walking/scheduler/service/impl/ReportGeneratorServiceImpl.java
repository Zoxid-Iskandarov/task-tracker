package com.walking.scheduler.service.impl;

import com.walking.scheduler.domain.model.Task;
import com.walking.scheduler.domain.projection.UserProjection;
import com.walking.scheduler.repository.TaskRepository;
import com.walking.scheduler.repository.UserRepository;
import com.walking.scheduler.service.MessageService;
import com.walking.scheduler.service.ReportGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportGeneratorServiceImpl implements ReportGeneratorService {
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    private static final int PAGE_SIZE = 100;

    @Override
    @Transactional(readOnly = true)
    public void generateDailyReport() {
        LocalDateTime since = LocalDateTime.now().minusDays(1L);
        Page<UserProjection> userProjections;
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);

        do {
            userProjections = userRepository.findUserProjectionsForDailyReport(since, pageable);

            if (userProjections.hasContent()) {
                processBatch(userProjections.getContent(), since);
            }

            pageable = pageable.next();
        } while (userProjections.hasNext());
    }

    private void processBatch(List<UserProjection> users, LocalDateTime since) {
        List<Long> userIds = users.stream()
                .map(UserProjection::id)
                .toList();

        List<Task> tasks = taskRepository.findRelevantForUsers(userIds, since);

        Map<Long, List<Task>> taskByUserId = tasks.stream()
                .collect(Collectors.groupingBy(task -> task.getUser().getId()));

        for (UserProjection user : users) {
            List<Task> userTasks = taskByUserId.getOrDefault(user.id(), Collections.emptyList());

            if (userTasks.isEmpty()) continue;

            List<Task> uncompleted = userTasks.stream()
                    .filter(t -> Boolean.FALSE.equals(t.getIsCompleted()))
                    .toList();

            List<Task> completedToday = userTasks.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getIsCompleted()))
                    .toList();

            if (uncompleted.isEmpty() && completedToday.isEmpty()) continue;

            messageService.buildAndSendMessage(user, uncompleted, completedToday);
        }
    }
}
