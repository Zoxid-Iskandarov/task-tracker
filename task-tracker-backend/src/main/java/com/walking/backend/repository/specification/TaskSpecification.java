package com.walking.backend.repository.specification;

import com.walking.backend.domain.model.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

@UtilityClass
public class TaskSpecification {

    public static Specification<Task> hasBoardId(Long boardId) {
        return (root, query, cb) ->
                cb.equal(root.get(Task_.section).get(Section_.board).get(Board_.id), boardId);
    }

    public static Specification<Task> hasSectionId(Long sectionId) {
        return (root, query, cb) -> {
            if (sectionId == null) return null;
            return cb.equal(root.get(Task_.section).get(Section_.id), sectionId);
        };
    }

    public static Specification<Task> hasTitle(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank()) return null;
            return cb.like(cb.lower(root.get(Task_.title)), "%" + title.toLowerCase() + "%");
        };
    }

    public static Specification<Task> hasCompleted(Boolean completed) {
        return (root, query, cb) -> {
            if (completed == null) return null;
            return cb.equal(root.get(Task_.isCompleted), completed);
        };
    }

    public static Specification<Task> hasLabels(List<Long> labelIds) {
        return (root, query, cb) -> {
            if (labelIds == null || labelIds.isEmpty()) return null;

            query.distinct(true);

            return root.join(Task_.labels).get(Label_.id).in(labelIds);
        };
    }

    public static Specification<Task> hasCreatedBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to == null) return cb.greaterThanOrEqualTo(root.get(Task_.created), from);
            if (from == null) return cb.lessThanOrEqualTo(root.get(Task_.created), to);
            return cb.between(root.get(Task_.created), from, to);
        };
    }
}
