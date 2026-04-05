package com.walking.backend.repository.specification;

import com.walking.backend.domain.model.Board_;
import com.walking.backend.domain.model.Section;
import com.walking.backend.domain.model.Section_;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class SectionSpecification {

    public static Specification<Section> hasBoardId(Long boardId) {
        return (root, query, cb) ->
                cb.equal(root.get(Section_.board).get(Board_.id), boardId);
    }
}
