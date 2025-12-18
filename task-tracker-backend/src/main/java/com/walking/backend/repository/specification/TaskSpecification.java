package com.walking.backend.repository.specification;

import com.walking.backend.domain.model.Task;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class TaskSpecification {

    public static Specification<Task> hasUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Task> isCompleted(Boolean completed) {
        return (root, query, cb) -> cb.equal(root.get("isCompleted"), completed);
    }

    public static Specification<Task> hasTodayFlag() {
        return (root, query, cb)
                -> cb.greaterThanOrEqualTo(root.get("created"), LocalDateTime.now().toLocalDate().atStartOfDay());
    }
}
