package com.walking.backend.repository.specification;

import com.walking.backend.domain.model.Section_;
import com.walking.backend.domain.model.Task;
import com.walking.backend.domain.model.Task_;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class TaskSpecification {

    public static Specification<Task> hasSectionId(Long sectionId) {
        return (root, query, cb) ->
                cb.equal(root.get(Task_.section).get(Section_.id), sectionId);
    }
}
