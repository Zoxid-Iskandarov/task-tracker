package com.walking.backend.repository.specification;

import com.walking.backend.domain.model.Task;
import com.walking.backend.domain.model.Task_;
import com.walking.backend.domain.model.User_;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class TaskSpecification {

    public static Specification<Task> hasUserId(Long userId) {
        return (root, query, cb)
                -> cb.equal(root.get(Task_.user).get(User_.id), userId);
    }

    public static Specification<Task> isCompleted(Boolean completed) {
        return (root, query, cb)
                -> cb.equal(root.get(Task_.isCompleted), completed);
    }

    public static Specification<Task> hasTodayFlag() {
        return (root, query, cb)
                -> cb.greaterThanOrEqualTo(root.get(Task_.created), LocalDateTime.now().toLocalDate().atStartOfDay());
    }
}
