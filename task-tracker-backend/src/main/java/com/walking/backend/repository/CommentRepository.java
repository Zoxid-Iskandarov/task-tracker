package com.walking.backend.repository;

import com.walking.backend.domain.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findAllByTaskId(Long taskId, Pageable pageable);

    Optional<Comment> findByIdAndTaskId(Long commentId, Long taskId);

    boolean existsByIdAndAuthorId(Long commentId, Long authorId);
}
