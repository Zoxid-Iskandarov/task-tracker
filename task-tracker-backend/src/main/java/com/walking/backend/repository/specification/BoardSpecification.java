package com.walking.backend.repository.specification;

import com.walking.backend.domain.model.Board;
import com.walking.backend.domain.model.Board_;
import com.walking.backend.domain.model.User_;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class BoardSpecification {

    public static Specification<Board> hasUserId(Long userId) {
        return (root, query, cb) ->
                cb.equal(root.get(Board_.user).get(User_.id), userId);
    }
}
