package com.walking.backend.security;

import com.walking.backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAccessChecker {
    private final TaskRepository taskRepository;

    public boolean isOwnerOfTask(Long taskId, Long userId) {
        return taskRepository.findById(taskId)
                .map(task -> task.getUser().getId().equals(userId))
                .orElse(false);
    }
}
