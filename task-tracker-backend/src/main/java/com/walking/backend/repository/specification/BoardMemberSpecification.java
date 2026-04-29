package com.walking.backend.repository.specification;

import com.walking.backend.domain.model.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

@UtilityClass
public class BoardMemberSpecification {

    public static Specification<BoardMember> hasBoardId(Long boardId) {
        return (root, query, cb) ->
                cb.equal(root.get(BoardMember_.board).get(Board_.id), boardId);
    }

    public static Specification<BoardMember> hasUsername(String username) {
        return (root, query, cb) -> {
            if (username == null || username.isBlank()) return null;
            return cb.like(
                    cb.lower(root.get(BoardMember_.user).get(User_.username)), "%" + username.toLowerCase() + "%");
        };
    }

    public static Specification<BoardMember> hasEmail(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isBlank()) return null;
            return cb.like(
                    cb.lower(root.get(BoardMember_.user).get(User_.email)), "%" + email.toLowerCase() + "%");
        };
    }

    public static Specification<BoardMember> hasRole(BoardRole role) {
        return (root, query, cb) -> {
            if (role == null) return null;
            return cb.equal(root.get(BoardMember_.role), role);
        };
    }

    public static Specification<BoardMember> hasJoinedBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (to == null) return cb.greaterThanOrEqualTo(root.get(BoardMember_.joined), from);
            if (from == null) return cb.lessThanOrEqualTo(root.get(BoardMember_.joined), to);

            return cb.between(root.get(BoardMember_.joined), from, to);
        };
    }
}
